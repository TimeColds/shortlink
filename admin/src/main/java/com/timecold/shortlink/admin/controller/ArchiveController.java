package com.timecold.shortlink.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.common.convention.result.Results;
import com.timecold.shortlink.admin.remote.dto.req.ArchiveRecoverReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ArchiveReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ArchiveRemoveReqDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.timecold.shortlink.admin.remote.service.RemoteShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 回收站管理控制层
 */
@RestController
@RequestMapping("/api/v1/short_link/admin")
@RequiredArgsConstructor
public class ArchiveController {

    private final RemoteShortLinkService remoteShortLinkService;
    /**
     * 归档短链接
     */
    @PutMapping("/archive")
    public Result<Void> archiveShortLink(@RequestBody ArchiveReqDTO requestParam) {
        remoteShortLinkService.archiveShortLink(requestParam);
        return Results.success();
    }

    /**
     * 分页查询归档的短链接
     */
    @GetMapping("/archived_links")
    public Result<Page<ShortLinkPageRespDTO>> pageArchivedShortLink(@RequestParam Long size, @RequestParam Long current) {
        return Results.success(remoteShortLinkService.pageArchivedShortLink(size, current));
    }

    @PutMapping("/archive_recover")
    public Result<Void> recoverShortLink(@RequestBody ArchiveRecoverReqDTO requestParam) {
        remoteShortLinkService.recoverShortLink(requestParam);
        return Results.success();
    }

    @PutMapping("/delete")
    public Result<Void> archiveRemove(@RequestBody ArchiveRemoveReqDTO requestParam) {
        remoteShortLinkService.archiveRemove(requestParam);
        return Results.success();
    }
}
