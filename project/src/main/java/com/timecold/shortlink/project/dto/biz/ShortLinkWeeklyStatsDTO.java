package com.timecold.shortlink.project.dto.biz;

import lombok.Data;

@Data
public class ShortLinkWeeklyStatsDTO {

    /**
     * 星期
     */
    private Integer dayOfWeek;

    /**
     * 页面访问量
     */
    private Long pv;

}
