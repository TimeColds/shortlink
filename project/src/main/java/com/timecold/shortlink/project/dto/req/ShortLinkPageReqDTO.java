package com.timecold.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.project.dao.entity.ShortLinkDO;
import lombok.Data;

/**
 * 短链接分页请求参数
 */
@Data
public class ShortLinkPageReqDTO extends Page<ShortLinkDO> {


    /**
     * 用户id
     */
    private Long uid;

    /**
     * 分组标识
     */
    private String gid;

}
