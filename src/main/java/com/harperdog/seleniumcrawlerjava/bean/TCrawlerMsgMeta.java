package com.harperdog.seleniumcrawlerjava.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriverService;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_crawler_msg_meta")
@ApiModel("聊天内容抓取元数据")
public class TCrawlerMsgMeta implements Serializable {
    /**
     * id
     */
    @ApiModelProperty("id")
    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 待抓取的直播间url
     */
    @ApiModelProperty("待抓取的直播间url")
    private String url;
    /**
     * 抓取的信息发送到的直播间的环信房间id
     */
    @ApiModelProperty("抓取的信息发送到的直播间的环信房间id")
    @TableField("huan_xin_room_id")
    private String huanxinRoomId;
    /**
     * 抓取开始时间
     */
    @ApiModelProperty("抓取开始时间")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date crawlerStartTime;
    /**
     * 抓取结束时间
     */
    @ApiModelProperty("抓取结束时间")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date crawlerEndTime;
    /**
     * 抓取间隙区间 单位ms，例如 1000-2000 表示休眠1-2秒之间的随机时间之后再抓
     */
    @ApiModelProperty("抓取间隙区间 单位ms，例如 1000-2000 表示休眠1-2秒之间的随机时间之后再抓")
    private String crawlerWaitRange;
    /**
     * 发送间隙区间 单位ms，例如 1000-2000 表示休眠1-2秒之间的随机时间之后再发下一条
     */
    @ApiModelProperty("发送间隙区间 单位ms，例如 1000-2000 表示休眠1-2秒之间的随机时间之后再发下一条")
    private String sendWaitRange;

    /**
     * 该任务是否有线程在执行 0没有，1有 默认0
     */
    @ApiModelProperty("该任务是否有线程在执行 0没有，1有 默认0")
    private Integer status;
    /**
     * 该任务的指令是停止还是执行 0停止 1执行 默认0
     */
    @ApiModelProperty("该任务的指令是停止还是执行 0停止 1执行 默认0")
    private Integer cmd;

    @ApiModelProperty("版本，每次修改+1")
    private Integer version;
    /**
     * 该任务当前使用的代理
     */
    @ApiModelProperty("该任务当前使用的代理")
    @TableField(exist=false)
    private String currProxy;
    /**
     * 当前代理有效期截止时间
     */
    @ApiModelProperty("当前代理有效期截止时间")
    @TableField(exist=false)
    private Date currProxyEndTime;
    /**
     * 该任务使用的浏览器窗口对象
     */
    @ApiModelProperty("该任务使用的浏览器窗口对象")
    @TableField(exist=false)
    private WebDriver webDriver;

    /**
     * 该任务使用的浏览器窗口驱动的管理服务
     */
    @ApiModelProperty("该任务使用的浏览器窗口驱动的管理服务")
    @TableField(exist=false)
    private ChromeDriverService driverService;
    /**
     * 该任务当前是哪个线程在执行
     */
    @ApiModelProperty("该任务当前是哪个线程在执行")
    @TableField(exist=false)
    private Thread currThread;
    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    private Date createTime;
    /**
     * 修改时间
     */
    @ApiModelProperty("修改时间")
    private Date updateTime;
    /**
     * 操作人
     */
    @ApiModelProperty("操作人")
    private String optUserId;

    @ApiModelProperty("备注")
    private String remark;

    /**
     * 品牌
     */
    private String brandCodes;

    /**
     * 品牌描述
     */
    @TableField(exist = false)
    private String brandDesc;

    /**
     * 环信账号标识
     */
    private String hxAccFlag;
}
