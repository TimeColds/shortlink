package com.timecold.shortlink.project.controller;

import com.timecold.shortlink.project.common.convention.result.Result;
import com.timecold.shortlink.project.common.convention.result.Results;
import com.timecold.shortlink.project.dto.req.RecycleBinArchiveReqDTO;
import com.timecold.shortlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 回收站管理控制层
 */
@RestController
@RequestMapping("/api/short-link/v1")
@RequiredArgsConstructor
public class RecycleBinController {

    private final RecycleBinService recycleBinService;

    /**
     * 归档短链接
     */
    @PutMapping("/archive")
    public Result<Void> archiveShortLink(@RequestBody RecycleBinArchiveReqDTO requestParam) {
        recycleBinService.archiveShortLink(requestParam);
        return Results.success();
    }
}

