package com.timecold.shortlink.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.project.common.convention.exception.ClientException;
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
@RequestMapping("/api/v1/short_link/stats")
@RequiredArgsConstructor
public class ShortLinkStatsController {

    private final ShortLinkStatsService shortLinkStatsService;

    @GetMapping("/daily_stats")
    public Result<ShortLinkDailyStatsRespDTO> dailyStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        dateValidation(beginDate, endDate);
        return Results.success(shortLinkStatsService.getDailyStats(shortUrl, beginDate, endDate));
    }

    @GetMapping("/chart_stats")
    public Result<ShortLinkChartStatsRespDTO> chartStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        dateValidation(beginDate, endDate);
        return Results.success(shortLinkStatsService.getChartStats(shortUrl, beginDate, endDate));
    }

    @GetMapping("/log_stats")
    public Result<Page<ShortLinkLogPageRespDTO>> logStats(ShortLinkLogPageReqDTO requestParam) {
        dateValidation(requestParam.getBeginDate(), requestParam.getEndDate());
        return Results.success(shortLinkStatsService.getLogStats(requestParam));
    }



    private void dateValidation(LocalDate beginDate, LocalDate endDate) {
        LocalDate now = LocalDate.now();
        if (beginDate.isAfter(endDate)) {
            throw new ClientException("开始日期不能大于结束日期");
        } else if (beginDate.isAfter(now) || endDate.isAfter(now)) {
            throw new ClientException("日期范围不能大于今天");
        }
    }
}
