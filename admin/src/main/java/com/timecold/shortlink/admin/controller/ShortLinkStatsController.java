package com.timecold.shortlink.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.admin.common.convention.exception.ClientException;
import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.common.convention.result.Results;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkLogPageReqDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkChartStatsRespDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkDailyStatsRespDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkLogPageRespDTO;
import com.timecold.shortlink.admin.remote.service.RemoteShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/short_link/admin/stats")
@RequiredArgsConstructor
public class ShortLinkStatsController {

    private final RemoteShortLinkService remoteShortLinkService;
    
    @GetMapping("/daily_stats")
    public Result<ShortLinkDailyStatsRespDTO> dailyStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        dateValidation(beginDate, endDate);
        return Results.success(remoteShortLinkService.getDailyStats(shortUrl, beginDate, endDate));
    }

    @GetMapping("/chart_stats")
    public Result<ShortLinkChartStatsRespDTO> chartStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        dateValidation(beginDate, endDate);
        return Results.success(remoteShortLinkService.getChartStats(shortUrl, beginDate, endDate));
    }

    @GetMapping("/log_stats")
    public Result<Page<ShortLinkLogPageRespDTO>> logStats(ShortLinkLogPageReqDTO requestParam) {
        dateValidation(requestParam.getBeginDate(), requestParam.getEndDate());
        return Results.success(remoteShortLinkService.getLogStats(requestParam));
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
