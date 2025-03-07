package com.timecold.shortlink.project.dto.biz;

import lombok.Data;

@Data
public class ShortLinkHourlyStatsDTO {

    /**
     * 星期
     */
    private Integer hour;

    /**
     * 页面访问量
     */
    private Long pv;

}
