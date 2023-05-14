package com.harperdog.seleniumcrawlerjava.utils;

import com.xxl.job.core.context.XxlJobHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogUtil {

    public static void success(String param){
        XxlJobHelper.log(param);
        log.info(param);
    }


    public static void fail(String param){
        XxlJobHelper.handleFail(param);
    }

}
