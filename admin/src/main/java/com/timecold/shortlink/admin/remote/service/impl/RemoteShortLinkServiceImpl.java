package com.timecold.shortlink.admin.remote.service.impl;

import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.timecold.shortlink.admin.remote.dto.resp.PageResponseDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.timecold.shortlink.admin.remote.service.RemoteShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class RemoteShortLinkServiceImpl implements RemoteShortLinkService {

    private final RestTemplate restTemplate;

    public static final String REMOTE_URL = "http://localhost:8001/api/short-link/v1";


    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String url = REMOTE_URL + "/create";

        ResponseEntity<Result<ShortLinkCreateRespDTO>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        new HttpEntity<>(requestParam),
                        new ParameterizedTypeReference<>() {}
                );
        return response.getBody().getData();
    }

    @Override
    public PageResponseDTO<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        String url = REMOTE_URL + "/page";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("current", requestParam.getCurrent())
                .queryParam("size", requestParam.getSize())
                .queryParam("gid", requestParam.getGid());
        // 发送GET请求并处理泛型响应
        ResponseEntity<Result<PageResponseDTO<ShortLinkPageRespDTO>>> response =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        return response.getBody().getData();
    }
}
