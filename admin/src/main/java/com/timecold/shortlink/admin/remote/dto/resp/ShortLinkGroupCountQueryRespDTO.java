package com.timecold.shortlink.admin.remote.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接分组查询返回参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkGroupCountQueryRespDTO {

    /**
     * 分组标识
     */
    private Long gid;

    /**
     * 短链接数量
     */
    private Long shortLinkCount;
}