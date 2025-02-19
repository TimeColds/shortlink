package com.timecold.shortlink.admin.controller;

import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.common.convention.result.Results;
import com.timecold.shortlink.admin.remote.dto.req.RecycleBinArchiveReqDTO;
import com.timecold.shortlink.admin.remote.service.RemoteShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回收站管理控制层
 */
@RestController
@RequestMapping("/api/short-link/admin/v1")
@RequiredArgsConstructor
public class RecycleBinController {

    private final RemoteShortLinkService remoteShortLinkService;
    /**
     * 归档短链接
     */
    @PutMapping("/archive")
    public Result<Void> archiveShortLink(@RequestBody RecycleBinArchiveReqDTO requestParam) {
        remoteShortLinkService.archiveShortLink(requestParam);
        return Results.success();
    }
}
