package com.harperdog.seleniumcrawlerjava.enums;

public enum CrawlerMsgTaskStatusEnum {
    running(1,"运行中"),
    stopped(0, "停止的"),
    ;

    private Integer code;
    private String desc;

    private CrawlerMsgTaskStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
