package com.timecold.shortlink.admin.remote.dto.req;

import lombok.Data;

/**
 * 短链接归档请求对象
 */
@Data
public class  ArchiveReqDTO {

    /**
     * 分组标识
     */
    private Long uid;

    /**
     * 短链接
     */
    private String shortUrl;

}
