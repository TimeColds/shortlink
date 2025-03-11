package com.timecold.shortlink.admin.remote.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.remote.dto.req.*;
import com.timecold.shortlink.admin.remote.dto.resp.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@FeignClient(value = "short-link-project")
public interface ShortLinkFeignClient {

    /**
     * 创建短链接
     *
     * @param requestParam 创建短链接请求参数
     * @return 短链接创建响应
     */
    @PostMapping("/api/v1/short_link/project/create")
    Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam);

    /**
     * 修改短链接
     *
     * @param requestParam 修改短链接请求参数
     */
    @PutMapping("/api/v1/short_link/project/update")
    Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam);

    /**
     * 分页查询短链接
     *
     * @param requestParam 分页短链接请求参数
     * @return 查询短链接响应
     */
    @GetMapping("/api/v1/short_link/project/page")
    Result<Page<ShortLinkPageRespDTO>> pageShortLink(@SpringQueryMap ShortLinkPageReqDTO requestParam);

    /**
     * 查询用户每个分组内短链接数量
     *
     * @return 每个分组内短链接数量响应
     */
    @GetMapping("/api/v1/short_link/project/count")
    Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(@RequestParam Long uid);

    /**
     * 根据 URL 获取标题
     *
     * @param url 目标网站地址
     * @return 网站标题
     */
    @GetMapping("/api/v1/short_link/project/title")
    Result<String> getTitleByUrl(@RequestParam String url);

    /**
     * 归档短链接
     *
     * @param requestParam 归档短链接请求参数
     */
    @PutMapping("/api/v1/short_link/project/archive")
    Result<Void> archiveShortLink(@RequestBody ArchiveReqDTO requestParam);

    /**
     * 分页查询归档的短链接
     *
     * @param size    每页显示条数
     * @param current 当前页
     * @return
     */
    @GetMapping("/api/v1/short_link/project/archived_links")
    Result<Page<ShortLinkPageRespDTO>> pageArchivedShortLink(@RequestParam Long size,
                                                     @RequestParam Long current,
                                                     @RequestParam Long uid);

    /**
     * 恢复归档的短链接
     *
     * @param requestParam 恢复归档的短链接请求参数
     */
    @PutMapping("/api/v1/short_link/project/archive_recover")
    Result<Void> recoverShortLink(@RequestBody ArchiveRecoverReqDTO requestParam);

    /**
     * 删除归档的短链接
     *
     * @param requestParam 删除归档的短链接请求参数
     */
    @PutMapping("/api/v1/short_link/project/delete")
    Result<Void> archiveRemove(@RequestBody ArchiveRemoveReqDTO requestParam);

    @GetMapping("/api/v1/short_link/project/stats/daily_stats")
    Result<ShortLinkDailyStatsRespDTO> getDailyStats(@RequestParam String shortUrl,
                                             @RequestParam LocalDate beginDate,
                                             @RequestParam LocalDate endDate);

    @GetMapping("/api/v1/short_link/project/stats/chart_stats")
    Result<ShortLinkChartStatsRespDTO> getChartStats(@RequestParam String shortUrl,
                                             @RequestParam LocalDate beginDate,
                                             @RequestParam LocalDate endDate);

    @GetMapping("/api/v1/short_link/project/stats/log_stats")
    Result<Page<ShortLinkLogPageRespDTO>> getLogStats(@SpringQueryMap ShortLinkLogPageReqDTO requestParam);

}
