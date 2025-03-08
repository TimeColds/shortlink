package com.timecold.shortlink.admin.remote.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链接分页返回参数
 */
@Data
public class ShortLinkLogPageRespDTO {

    private Long id;

    /**
     * 短链接
     */
    private String shortUrl;

    /**
     * 访问时间
     */
    private LocalDateTime accessTime;

    /**
     * 访问者IP地址
     */
    private String ip;

    /**
     * 来源页面
     */
    private String referer;

    /**
     * 设备类型
     */
    private String device;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 访客类型
     */
    private Integer visitorType;
}
