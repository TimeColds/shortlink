package com.timecold.shortlink.project.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.project.common.convention.result.Result;
import com.timecold.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkPageRespDTO;

public class CustomBlockHandler {
    public static Result<ShortLinkCreateRespDTO> createShortLinkBlockHandlerMethod(ShortLinkCreateReqDTO requestParam, BlockException exception) {
        return new Result<ShortLinkCreateRespDTO>().setCode("B100000").setMessage("创建过于频繁，请稍候再试...");
    }

    public static Result<Page<ShortLinkPageRespDTO>> pageShortLinkBlockHandlerMethod(ShortLinkPageReqDTO requestParam, BlockException exception) {
        return new Result<Page<ShortLinkPageRespDTO>>().setCode("B100000").setMessage("查询过于频繁，请稍候再试...");
    }
}