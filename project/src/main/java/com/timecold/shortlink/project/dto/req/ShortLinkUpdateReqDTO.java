package com.timecold.shortlink.project.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 短链接修改请求对象
 */
@Data
public class ShortLinkUpdateReqDTO {
    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 短链接
     */
    private String shortUrl;

    /**
     * 原始分组标识
     */
    private Long originGid;

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
     * 描述
     */
    private String describe;
}
