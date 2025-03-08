package com.timecold.shortlink.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public void archiveShortLink(@RequestBody ArchiveReqDTO requestParam) {
        archiveService.archiveShortLink(requestParam);
    }

    /**
     * 分页查询归档的短链接
     */
    @GetMapping("/archived_links")
    public Page<ShortLinkPageRespDTO> pageArchivedShortLink(ArchivedPageReqDTO requestParam) {
        return archiveService.pageArchivedShortLink(requestParam);
    }

    @PutMapping("/archive_recover")
    public void recoverShortLink(@RequestBody ArchiveRecoverReqDTO requestParam) {
        archiveService.recoverShortLink(requestParam);
    }

    @PutMapping("/delete")
    public void archiveRemove(@RequestBody ArchiveRemoveReqDTO requestParam) {
        archiveService.archiveRemove(requestParam);
    }
}

