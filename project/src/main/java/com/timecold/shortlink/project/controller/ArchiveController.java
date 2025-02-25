package com.timecold.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.timecold.shortlink.project.common.convention.result.Result;
import com.timecold.shortlink.project.common.convention.result.Results;
import com.timecold.shortlink.project.dto.req.ArchiveRecoverReqDTO;
import com.timecold.shortlink.project.dto.req.ArchiveRemoveReqDTO;
import com.timecold.shortlink.project.dto.req.ArchiveReqDTO;
import com.timecold.shortlink.project.dto.req.ArchivedPageReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.timecold.shortlink.project.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 回收站管理控制层
 */
@RestController
@RequestMapping("/api/v1/short_link")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    /**
     * 归档短链接
     */
    @PutMapping("/archive")
    public Result<Void> archiveShortLink(@RequestBody ArchiveReqDTO requestParam) {
        archiveService.archiveShortLink(requestParam);
        return Results.success();
    }

    /**
     * 分页查询归档的短链接
     */
    @GetMapping("/archived_links")
    public Result<IPage<ShortLinkPageRespDTO>> pageArchivedShortLink(ArchivedPageReqDTO requestParam) {
        return Results.success(archiveService.pageArchivedShortLink(requestParam));
    }

    @PutMapping("/archive_recover")
    public Result<Void> recoverShortLink(@RequestBody ArchiveRecoverReqDTO requestParam) {
        archiveService.recoverShortLink(requestParam);
        return Results.success();
    }

    @PutMapping("/delete")
    public Result<Void> archiveRemove(@RequestBody ArchiveRemoveReqDTO requestParam) {
        archiveService.archiveRemove(requestParam);
        return Results.success();
    }
}

