package com.harperdog.seleniumcrawlerjava.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.harperdog.seleniumcrawlerjava.bean.TCrawlerMsgMeta;
import com.harperdog.seleniumcrawlerjava.consts.CrawlerMsgConst;
import com.harperdog.seleniumcrawlerjava.mapper.TCrawlerMsgMetaMapper;
import com.harperdog.seleniumcrawlerjava.service.CrawlerMsgService;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;

@Service("crawlerMsgService")
@Primary
@Slf4j
public class CrawlerMsgServiceImpl implements CrawlerMsgService {

    @Autowired(required = false)
    private TCrawlerMsgMetaMapper tCrawlerMsgMetaMapper;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Override
    public void updateTaskStatus(Integer status, Integer id) {
        if (id == null || status == null) {
            return;
        }
        //修改内存map
        TCrawlerMsgMeta tCrawlerMsgMeta = CrawlerMsgConst.crawlerMsgTaskConMap.get(id);
        tCrawlerMsgMeta.setStatus(status);
        //修改数据库
        tCrawlerMsgMetaMapper.update(null, Wrappers.<TCrawlerMsgMeta>lambdaUpdate()
                .eq(TCrawlerMsgMeta::getId, id)
                .set(TCrawlerMsgMeta::getStatus, status)
        );
    }

    @Override
    public List<TCrawlerMsgMeta> queryAll() {
        return tCrawlerMsgMetaMapper.selectList(null);
    }

    /**
     * 获取单个时效29-30分钟的代理
     * 返回格式为 ip:port
     *
     * @return
     */
    @Override
    public String getSingleProxy() {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity("http://www.zdopen.com/PrivateProxy/GetIP/?api=202111091412523879&akey=c4a727ac8d3157c5&count=1&fitter=2&timespan=29&type=1", String.class);
        return responseEntity.getBody();
    }


    @Override
    public WebDriver createWebDriver(TCrawlerMsgMeta task) throws Exception{
        log.info(String.format("任务id:%s 创建web窗口", task.getId()));
        if (task == null) {
            return null;
        }

        try {
            //关闭之前的窗口
            if (task.getWebDriver() != null) {
                task.getWebDriver().quit();
            }

            if(task.getDriverService() != null){
                task.getDriverService().stop();
            }
        } catch (Exception e) {
            log.warn("WebDriver close window: value{}", e);
        }
        //返回的代理有效期是29-30
        ResponseEntity<String> responseEntity = restTemplate.getForEntity("代理地址", String.class);
        String proxy = responseEntity.getBody();
        task.setCurrProxyEndTime(new Date(System.currentTimeMillis() + 29 * 60 * 1000));
        task.setCurrProxy(proxy);

        System.setProperty("webdriver.gecko.driver", "/home/java_test/geckodriver");


        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-dev-shm-usage");
        //设置个代理
        options.setProxy(new Proxy().setHttpProxy(proxy).setSslProxy(proxy));

        WebDriver webDriver = new FirefoxDriver(options);

        webDriver.get(task.getUrl());

        task.setWebDriver(webDriver);

        return webDriver;
    }
}
