package com.timecold.shortlink.admin.remote.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 短链接分页请求参数
 */
@Data
public class ShortLinkPageReqDTO extends Page {


    /**
     * 用户id
     */
    private Long uid;

    /**
     * 分组标识
     */
    private String gid;

}
