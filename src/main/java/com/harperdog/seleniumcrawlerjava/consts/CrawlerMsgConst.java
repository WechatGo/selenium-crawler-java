package com.harperdog.seleniumcrawlerjava.consts;


import com.harperdog.seleniumcrawlerjava.bean.TCrawlerMsgMeta;

import java.util.concurrent.ConcurrentHashMap;

public class CrawlerMsgConst {
    //需要抓取的任务的并发map
    public static ConcurrentHashMap<Integer, TCrawlerMsgMeta> crawlerMsgTaskConMap = new ConcurrentHashMap<>();
}
