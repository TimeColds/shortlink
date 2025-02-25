package com.timecold.shortlink.project.dto.req;

import lombok.Data;

@Data
public class ArchiveRemoveReqDTO {

    /**
     * 用户id
     */
    private Long uid;

    /**
     * 短链接
     */
    private String shortUrl;
}
