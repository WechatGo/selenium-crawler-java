package com.harperdog.seleniumcrawlerjava.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerMsgBean implements Serializable {
    /**
     * 发送人昵称
     */
    private String userNick;
    /**
     * 消息
     */
    private String msg;
}
