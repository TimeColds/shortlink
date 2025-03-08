package com.timecold.shortlink.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.project.dto.req.ShortLinkLogPageReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkChartStatsRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkDailyStatsRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkLogPageRespDTO;
import com.timecold.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/short_link/stats")
@RequiredArgsConstructor
public class ShortLinkStatsController {

    private final ShortLinkStatsService shortLinkStatsService;

    @GetMapping("/daily_stats")
    public ShortLinkDailyStatsRespDTO dailyStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        return shortLinkStatsService.getDailyStats(shortUrl, beginDate, endDate);
    }

    @GetMapping("/chart_stats")
    public ShortLinkChartStatsRespDTO chartStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        return shortLinkStatsService.getChartStats(shortUrl, beginDate, endDate);
    }

    @GetMapping("/log_stats")
    public Page<ShortLinkLogPageRespDTO> logStats(ShortLinkLogPageReqDTO requestParam) {
        return shortLinkStatsService.getLogStats(requestParam);
    }



}
