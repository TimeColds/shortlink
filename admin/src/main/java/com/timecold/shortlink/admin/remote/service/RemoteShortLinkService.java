package com.timecold.shortlink.admin.remote.service;

import com.timecold.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.timecold.shortlink.admin.remote.dto.resp.PageResponseDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

public interface RemoteShortLinkService {


    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

    PageResponseDTO<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);
}
