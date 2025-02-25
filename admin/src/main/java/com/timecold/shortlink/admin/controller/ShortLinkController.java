package com.timecold.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.common.convention.result.Results;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.timecold.shortlink.admin.remote.service.RemoteShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/short_link/admin")
@RequiredArgsConstructor
public class ShortLinkController {

    private final RemoteShortLinkService remoteShortLinkService;


    /**
     * 创建短链接
     */
    @PostMapping("/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        return remoteShortLinkService.createShortLink(requestParam);
    }

    /**
     * 修改短链接
     */
    @PutMapping("/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        remoteShortLinkService.updateShortLink(requestParam);
        return Results.success();
    }

    /**
     * 分页查询短链接
     */
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
        return remoteShortLinkService.pageShortLink(requestParam);
    }

    /**
     * 根据 URL 获取标题
     */
    @GetMapping("/title")
    public Result<String> getTitleByUrl(@RequestParam("url") String url) {
        return Results.success(remoteShortLinkService.getTitleByUrl(url));
    }
}
