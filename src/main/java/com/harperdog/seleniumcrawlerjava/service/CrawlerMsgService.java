package com.harperdog.seleniumcrawlerjava.service;

import com.harperdog.seleniumcrawlerjava.bean.TCrawlerMsgMeta;

import java.util.List;

public interface CrawlerMsgService {
    /**
     * 修改任务状态
     * @param status
     * @param id
     */
    void updateTaskStatus(Integer status, Integer id);

    /**
     * 获取数据库中所有抓取任务
     * @return
     */
    List<TCrawlerMsgMeta> queryAll();

    /**
     * 获取单条的代理 代理失效在 29-30分钟
     * @return
     */
    String getSingleProxy();

    /**
     * 创建浏览器窗口对象
     * @param task
     * @return
     */
    WebDriver createWebDriver(TCrawlerMsgMeta task) throws Exception;
}
