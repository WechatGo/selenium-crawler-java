package com.harperdog.seleniumcrawlerjava.handler;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.harperdog.seleniumcrawlerjava.bean.CrawlerMsgBean;
import com.harperdog.seleniumcrawlerjava.bean.TCrawlerMsgMeta;
import com.harperdog.seleniumcrawlerjava.consts.CrawlerMsgConst;
import com.harperdog.seleniumcrawlerjava.enums.CrawlerMsgCmdEnum;
import com.harperdog.seleniumcrawlerjava.enums.CrawlerMsgTaskStatusEnum;
import com.harperdog.seleniumcrawlerjava.service.CrawlerMsgService;
import com.harperdog.seleniumcrawlerjava.service.ReptileRiskControlService;
import com.harperdog.seleniumcrawlerjava.utils.BeanCopierUtils;
import com.harperdog.seleniumcrawlerjava.utils.LogUtil;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 抓取三方直播间聊天信息的定时任务
 */
@Service
@Slf4j
public class CrawlerMsgMateScanScheduler {

    @Autowired
    private CrawlerMsgService crawlerMsgService;

    /**
     * 任务运行状态 0未运行，1正在运行
     * 部署单个节点可以这样通过声明静态变量控制任务的重复调度
     */
    private static int scanAndRunStatus = 0;

    @Resource
    private ReptileRiskControlService reptileRiskControlService;


    /**
     * 扫描db数据，启动线程执行消息抓取和发送
     * <p>
     * 暂定10秒一次
     */
    @XxlJob("CrawlerMsgScan")
    public void scanAndRun() {
        try {
            log.info("CrawlerMsgMateScanScheduler#scanAndRun start");
            //判断该任务是否运行，若已运行直接返回
            if (scanAndRunStatus == 0) {
                scanAndRunStatus = 1;
            } else {
                log.info("CrawlerMsgMateScanScheduler#scanAndRun is already running");
                return;
            }

            //获取表中所有的数据
            List<TCrawlerMsgMeta> dbTasks = crawlerMsgService.queryAll();

            //从内存中移除db中没有的任务
            removeTaskNotInDb(dbTasks);

            for (TCrawlerMsgMeta dbTask : dbTasks) {

                long currMill = System.currentTimeMillis();
                //将db任务更新到内存
                TCrawlerMsgMeta mmTask = CrawlerMsgConst.crawlerMsgTaskConMap.get(dbTask.getId());
                if (mmTask == null) {
                    //创建task对象塞到currMap
                    mmTask = BeanCopierUtils.copy(dbTask, TCrawlerMsgMeta.class);
                    CrawlerMsgConst.crawlerMsgTaskConMap.put(dbTask.getId(), mmTask);
                } else {
                    if (!dbTask.getVersion().equals(mmTask.getVersion())) {
                        updateMmTask(dbTask, mmTask);
                    }
                    //这里需要同步数据库指令
                    mmTask.setCmd(dbTask.getCmd());
                }

                // 如果任务符合执行条件，则保证有线程执行该任务
                if (currMill <= mmTask.getCrawlerEndTime().getTime()
                        && currMill >= mmTask.getCrawlerStartTime().getTime()
                        && CrawlerMsgCmdEnum.run.getCode().equals(mmTask.getCmd())) {

                    Thread currThread = mmTask.getCurrThread();
                    if (currThread == null || !currThread.isAlive()) {

                        if (mmTask.getWebDriver() == null) {
                            crawlerMsgService.createWebDriver(mmTask);
                        }

                        //创建新线程thread执行该task
                        TCrawlerMsgMeta exeTask = mmTask;
                        Thread thread = startNewThread(mmTask);

                        //将thread塞到task
                        mmTask.setCurrThread(thread);
                        //启动线程
                        thread.start();
                    }

                }
                //如果该任务不符合执行条件，则停止任务
                else {
                    //如果没有线程执行该任务，则在这设置任务状态为停止的；如果有线程执行则交由线程处理
                    if (mmTask.getCurrThread() == null || mmTask.getCurrThread().isAlive() == false) {
                        closeChrome(mmTask);
                        crawlerMsgService.updateTaskStatus(CrawlerMsgTaskStatusEnum.stopped.getCode(), mmTask.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("CrawlerMsgMateScanScheduler#scanAndRun failed", e);
            LogUtil.fail("CrawlerMsgMateScanScheduler#scanAndRun failed");
        } finally {
            //结束运行
            scanAndRunStatus = 0;

            LogUtil.success("CrawlerMsgMateScanScheduler#scanAndRun end");
        }
    }

    private void crawlerAndSendRun(TCrawlerMsgMeta task) {
        try {

            if (task == null || task.getId() == null) {
                return;
            }

            //设置任务在运行中
            crawlerMsgService.updateTaskStatus(CrawlerMsgTaskStatusEnum.running.getCode(), task.getId());


            //获取抓取的等待时间区间，单位ms
            String crawlerWaitRange = task.getCrawlerWaitRange();
            String[] sendWaitRangeArr = crawlerWaitRange.split("-");
            int minWait = Integer.parseInt(sendWaitRangeArr[0]);
            int maxWait = Integer.parseInt(sendWaitRangeArr[1]);

            Random random = new Random();


            List<CrawlerMsgBean> preMsgList = new ArrayList<>();
            while (true) {
                long start = System.currentTimeMillis();
                if (task == null || task.getCmd() != 1
                        || start < task.getCrawlerStartTime().getTime()
                        || start > task.getCrawlerEndTime().getTime()
                ) {
                    return;
                }

                if (start > task.getCurrProxyEndTime().getTime()) {
                    // 重新获取代理ip，重新打开窗口，并赋值给task
                    crawlerMsgService.createWebDriver(task);
                }

                //抓取聊天室内容
                List<CrawlerMsgBean> currMsgList = crawlerMsg(task);
                //获取上次没有发送的聊天内容
                List<CrawlerMsgBean> newMsgList = filterNewMsgList(currMsgList, preMsgList);
                //本次抓取的聊天内容赋值给preList
                preMsgList = currMsgList;
                sendMsg(newMsgList, task);
                //等待
                //执行时间
                long diffTime = System.currentTimeMillis() - start;
                int waitTime = random.nextInt(minWait) % (maxWait - minWait + 1) + minWait;
                long sleepTime = waitTime - diffTime;

                if (sleepTime > 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        log.warn("", e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn(String.format("抓取任务异常：%s", JSONObject.toJSONString(task)), e);
        } finally {
            //设置任务停止运行，任务停止关闭窗口
            closeChrome(task);
            crawlerMsgService.updateTaskStatus(CrawlerMsgTaskStatusEnum.stopped.getCode(), task.getId());
        }
    }

    private List<CrawlerMsgBean> crawlerMsg(TCrawlerMsgMeta task) {
        WebDriver webDriver = task.getWebDriver();
        List<WebElement> elements = webDriver.findElements(By.xpath("//li[@class=\"start\" or @class=\"start welcome\"]"));

        List<CrawlerMsgBean> msgList = new ArrayList<>();

        if (CollectionUtils.isEmpty(elements)) {
            //获取不到值时， 刷新
            task.getWebDriver().navigate().refresh();
            return msgList;
        }

        for (WebElement element : elements) {
            WebElement name = element.findElement(By.xpath("span[@class=\"name\"]"));
            WebElement msg = element.findElement(By.xpath("span[@class=\"content-txt\"]"));


            msgList.add(new CrawlerMsgBean(name.getText().replace("：", ""), msg.getText().replace("进入聊天室"," 进入直播间")));

        }

        return msgList;
    }

    private List<CrawlerMsgBean> filterNewMsgList(List<CrawlerMsgBean> currMsgList, List<CrawlerMsgBean> preMsgList) {
        Stack<CrawlerMsgBean> msgStack = new Stack<>();
        List<CrawlerMsgBean> newMsgList = new ArrayList<>();

        if (CollectionUtils.isEmpty(currMsgList)) {
            return newMsgList;
        }

        if (CollectionUtils.isEmpty(preMsgList)) {
            return currMsgList;
        }

        for (CrawlerMsgBean msg : currMsgList) {
            if (!preMsgList.contains(msg)) {
                msgStack.push(msg);
            }
        }

        while (msgStack.size() > 0) {
            newMsgList.add(msgStack.pop());
        }

        return newMsgList;
    }

    private void sendMsg(List<CrawlerMsgBean> newMsgList, TCrawlerMsgMeta task) {
        if (CollectionUtils.isEmpty(newMsgList)) {
            return;
        }
        String sendWaitRange = task.getSendWaitRange();

        //计算发送等待时间
        String[] sendWaitRangeArr = sendWaitRange.split("-");
        int minWait = Integer.parseInt(sendWaitRangeArr[0]);
        int maxWait = Integer.parseInt(sendWaitRangeArr[1]);

        Random random = new Random();

        for (CrawlerMsgBean msg : newMsgList) {

            long start = System.currentTimeMillis();
            if (task == null || task.getCmd() != 1
                    || start < task.getCrawlerStartTime().getTime()
                    || start > task.getCrawlerEndTime().getTime()
            ) {
                continue;
            }

            //消息过风控
            boolean canSend = reptileRiskControlService.riskControl(msg.getMsg(), msg.getUserNick());
            if (!canSend) {
                log.info("msg or userNick invalide");
                continue;
            }
            try {
                sendMsgRoom(msg, task);
            } catch (Exception e) {
                log.warn("发送环信消息失败", e);
            }
            int waitTime = random.nextInt(minWait) % (maxWait - minWait + 1) + minWait;

            try {
                TimeUnit.MILLISECONDS.sleep(waitTime);
            } catch (InterruptedException e) {
                log.warn("", e);
            }
        }

    }

    /**
     * 发送采集到的消息
     * @param msgBean
     * @param task
     */
    private void sendMsgRoom(CrawlerMsgBean msgBean, TCrawlerMsgMeta task) {


    }

    private void updateMmTask(TCrawlerMsgMeta dbTask, TCrawlerMsgMeta currMmTask) throws Exception{
        if (currMmTask == null || dbTask == null) {
            return;
        }
        String currUrl = currMmTask.getUrl();

        //如果url有变化，重新拉代理ip，重新创建浏览器窗口，重新打开页面
//        if (!dbTask.getUrl().equals(currUrl)) {
//            currMmTask.setUrl(dbTask.getUrl());
//            crawlerMsgService.createWebDriver(currMmTask);
//        }

        //不论url有没有变都重开窗口，兼容抓不到消息需要重开窗口的情况
        currMmTask.setUrl(dbTask.getUrl());
        //有线程在跑则重新创建
        if(currMmTask.getCurrThread() != null && currMmTask.getCurrThread().isAlive()){
            crawlerMsgService.createWebDriver(currMmTask);
        }

        if (StringUtils.isNotBlank(dbTask.getRoomId())) {
            currMmTask.setRoomId(dbTask.getRoomId());
        }

        if (dbTask.getCrawlerStartTime() != null) {
            currMmTask.setCrawlerStartTime(dbTask.getCrawlerStartTime());
        }

        if (dbTask.getCrawlerEndTime() != null) {
            currMmTask.setCrawlerEndTime(dbTask.getCrawlerEndTime());
        }

        if (StringUtils.isNotBlank(dbTask.getCrawlerWaitRange())) {
            currMmTask.setCrawlerWaitRange(dbTask.getCrawlerWaitRange());
        }

        if (StringUtils.isNotBlank(dbTask.getSendWaitRange())) {
            currMmTask.setSendWaitRange(dbTask.getSendWaitRange());
        }

        if(StringUtils.isNotBlank(dbTask.getHxAccFlag())){
            currMmTask.setHxAccFlag(dbTask.getHxAccFlag());
        }

        if(StringUtils.isNotBlank(dbTask.getBrandCodes())){
            currMmTask.setBrandCodes(dbTask.getBrandCodes());
        }

        if (dbTask.getCmd() != null) {
            currMmTask.setCmd(dbTask.getCmd());
        }
    }

    /**
     * 启动新线程执行任务
     *
     * @param task
     * @return
     */
    private Thread startNewThread(TCrawlerMsgMeta task) {
        Thread thread = new Thread(() -> {
            crawlerAndSendRun(task);
        }, "Thread-抓聊天信息-" + task.getId());
        return thread;
    }

    /**
     * 从内存中移除db中没有的任务
     *
     * @param dbTasks
     */
    private void removeTaskNotInDb(List<TCrawlerMsgMeta> dbTasks) {
        Set<Integer> dbIdSet = dbTasks.stream().map(var -> {
            return var.getId();
        }).collect(Collectors.toSet());

        Iterator<Integer> iterator = CrawlerMsgConst.crawlerMsgTaskConMap.keySet().iterator();
        while (iterator.hasNext()) {
            Integer mmId = iterator.next();

            if (!dbIdSet.contains(mmId)) {
                //关闭之前的窗口
                TCrawlerMsgMeta mmTask = CrawlerMsgConst.crawlerMsgTaskConMap.get(mmId);
                closeChrome(mmTask);
                CrawlerMsgConst.crawlerMsgTaskConMap.remove(mmId);
            }
        }
    }

    /**
     * 关闭chrome进程
     * @param mmTask
     */
    private void closeChrome(TCrawlerMsgMeta mmTask){
        if(mmTask == null){
            return;
        }

        if (mmTask.getWebDriver() != null) {
            try {
                mmTask.getWebDriver().quit();
                mmTask.setWebDriver(null);
            } catch (Exception e) {
                log.warn("", e);
            }
        }

        if (mmTask.getDriverService() != null) {
            try {
                mmTask.getDriverService().stop();
                mmTask.setDriverService(null);
            } catch (Exception e) {
                log.warn("", e);
            }
        }
    }

}


