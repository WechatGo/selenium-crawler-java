package com.harperdog.seleniumcrawlerjava.enums;

public enum CrawlerMsgCmdEnum {
    run(1,"运行"),
    stop(0, "停止"),
    ;

    private Integer code;
    private String desc;

    private CrawlerMsgCmdEnum(Integer code, String desc) {
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
