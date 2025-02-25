package com.timecold.shortlink.project.dto.req;

import lombok.Data;

@Data
public class ArchiveRecoverReqDTO {

    /**
     * 用户id
     */
    private Long uid;

    /**
     * 分组标识
     */
    private Long gid;

    /**
     * 短链接
     */
    private String shortUrl;
}
