package com.timecold.shortlink.project.service;

import com.timecold.shortlink.project.dto.resp.ShortLinkChartStatsRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkDailyStatsRespDTO;

import java.time.LocalDate;

/**
 * 短链接统计接口层
 */
public interface ShortLinkStatsService {

    /**
     *
     * @param shortUrl
     * @param beginDate
     * @param endDate
     * @return
     */
    ShortLinkDailyStatsRespDTO getDailyStats(String shortUrl, LocalDate beginDate, LocalDate endDate);

    /**
     *
     * @param shortUrl
     * @param beginDate
     * @param endDate
     * @return
     */
    ShortLinkChartStatsRespDTO getChartStats(String shortUrl, LocalDate beginDate, LocalDate endDate);
}
