package com.timecold.shortlink.project.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.timecold.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.timecold.shortlink.project.handler.CustomBlockHandler;
import com.timecold.shortlink.project.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/short_link")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    /**
     * 创建短链接
     */
    @PostMapping("/create")
    @SentinelResource(
            value = "create_short-link",
            blockHandler = "createShortLinkBlockHandlerMethod",
            blockHandlerClass = CustomBlockHandler.class
    )
    public ShortLinkCreateRespDTO createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        return shortLinkService.createShortLink(requestParam);
    }


    /**
     * 修改短链接
     */
    @PutMapping("/update")
    public void updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        shortLinkService.updateShortLink(requestParam);
    }

    /**
     * 分页查询短链接
     */
    @GetMapping("/page")
    public Page<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        return shortLinkService.pageShortLink(requestParam);
    }

    /**
     * 查询短链接分组内数量
     */
    @GetMapping("/count")
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(@RequestParam("uid") Long uid) {
        return shortLinkService.listGroupShortLinkCount(uid);
    }

    /**
     * 根据 URL 获取对应网站的标题
     */
    @GetMapping("/title")
    public String getTitleByUrl(@RequestParam("url") String url) {
        return shortLinkService.getTitleByUrl(url);
    }
}
