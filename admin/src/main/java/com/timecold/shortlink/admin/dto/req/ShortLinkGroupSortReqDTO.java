package com.timecold.shortlink.admin.dto.req;

import lombok.Data;

import java.util.List;

/**
 * 短链接分组排序参数
 */
@Data
public class ShortLinkGroupSortReqDTO {

    /**
     * 排序
     */
    private List<Long> gidList;
}
