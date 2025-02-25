package com.timecold.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.project.dao.entity.ShortLinkDO;
import lombok.Data;

@Data
public class ArchivedPageReqDTO extends Page<ShortLinkDO> {

    /**
     * 用户id
     */
    private Long uid;
}
