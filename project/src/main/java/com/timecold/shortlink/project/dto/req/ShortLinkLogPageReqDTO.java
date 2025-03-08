package com.timecold.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.project.dao.entity.LinkLogStatsDO;
import lombok.Data;

import java.time.LocalDate;

/**
 * 短链接分页请求参数
 */
@Data
public class ShortLinkLogPageReqDTO extends Page<LinkLogStatsDO> {

    /**
     * 分组标识
     */
    private String shortUrl;

    private LocalDate beginDate;

    private LocalDate endDate;

}
