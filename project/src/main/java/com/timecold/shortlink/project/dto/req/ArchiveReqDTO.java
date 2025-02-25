package com.timecold.shortlink.project.dto.req;

import lombok.Data;

/**
 * 短链接归档请求对象
 */
@Data
public class ArchiveReqDTO {

    /**
     * 用户id
     */
    private Long uid;

    /**
     * 短链接
     */
    private String shortUrl;

}
