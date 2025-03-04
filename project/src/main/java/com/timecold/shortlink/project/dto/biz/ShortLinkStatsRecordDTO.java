package com.timecold.shortlink.project.dto.biz;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 短链接统计实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortLinkStatsRecordDTO {

    /**
     * 短链接
     */
    private String shortUrl;

    /**
     * 访问者唯一标识
     */
    private String userIdentifier;

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
     * 用户代理
     */
    private String userAgent;
}
