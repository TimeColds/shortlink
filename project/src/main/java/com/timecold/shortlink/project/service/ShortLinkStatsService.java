package com.timecold.shortlink.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.project.dto.req.ShortLinkLogPageReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkChartStatsRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkDailyStatsRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkLogPageRespDTO;

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

    Page<ShortLinkLogPageRespDTO> getLogStats(ShortLinkLogPageReqDTO requestParam);
}
