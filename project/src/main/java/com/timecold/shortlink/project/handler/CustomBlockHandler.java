package com.timecold.shortlink.project.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.timecold.shortlink.project.common.convention.exception.ServiceException;
import com.timecold.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkCreateRespDTO;

public class CustomBlockHandler {
    public static ShortLinkCreateRespDTO createShortLinkBlockHandlerMethod(ShortLinkCreateReqDTO requestParam, BlockException exception) {
        throw new ServiceException("当前访问网站人数过多，请稍后再试...");
    }
}