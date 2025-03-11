package com.timecold.shortlink.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.project.common.convention.result.Result;
import com.timecold.shortlink.project.common.convention.result.Results;
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
@RequestMapping("/api/v1/short_link/project")
@RequiredArgsConstructor
public class ShortLinkStatsController {

    private final ShortLinkStatsService shortLinkStatsService;

    @GetMapping("/stats/daily_stats")
    public Result<ShortLinkDailyStatsRespDTO> dailyStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        return Results.success(shortLinkStatsService.getDailyStats(shortUrl, beginDate, endDate));
    }

    @GetMapping("/stats/chart_stats")
    public Result<ShortLinkChartStatsRespDTO> chartStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        return Results.success(shortLinkStatsService.getChartStats(shortUrl, beginDate, endDate));
    }

    @GetMapping("/stats/log_stats")
    public Result<Page<ShortLinkLogPageRespDTO>> logStats(ShortLinkLogPageReqDTO requestParam) {
        return Results.success(shortLinkStatsService.getLogStats(requestParam));
    }



}
