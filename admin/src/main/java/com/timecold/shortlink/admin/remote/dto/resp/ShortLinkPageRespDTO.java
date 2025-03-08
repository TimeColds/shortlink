package com.timecold.shortlink.admin.remote.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 短链接分页返回参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkPageRespDTO {

    /**
     * id
     */
    private Long id;

    /**
     * 域名
     */
    private String domain;

    /**
     * 短链接
     */
    private String shortUrl;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 分组标识
     */
    private Long gid;

    /**
     * 用户id
     */
    private Long uid;

    /**
     * 有效期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 描述
     */
    private String describe;

    /**
     * 网站标识
     */
    private String favicon;

    private todayStats todayStats;

    private allStats allStats;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class todayStats {
        private Long pv;
        private Long uv;
        private Long uip;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class allStats {
        private Long pv;
        private Long uv;
        private Long uip;
    }
}
