package com.timecold.shortlink.admin.remote.service.impl;

import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.timecold.shortlink.admin.remote.service.RemoteShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RemoteShortLinkServiceImpl implements RemoteShortLinkService {

    private final RestTemplate restTemplate;


    public static final String REMOTE_URL = "http://localhost:8001/api/short-link/v1";


    @Override
    public Result createShortLink(ShortLinkCreateReqDTO requestParam) {
        String url = REMOTE_URL + "/create";
        return restTemplate.postForObject(url, requestParam, Result.class);
    }

    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        String url = REMOTE_URL + "/update";
        restTemplate.put(url, requestParam);
    }

    @Override
    public Result pageShortLink(ShortLinkPageReqDTO requestParam) {
        String url = REMOTE_URL + "/page";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("gid", requestParam.getGid())  // 添加其他参数
                .queryParam("size", requestParam.getSize())
                .queryParam("current", requestParam.getCurrent());
        String finalUrl = builder.toUriString();
        return restTemplate.getForObject(finalUrl, Result.class);
    }

    @Override
    public Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(List<String> requestParam) {
        String url = REMOTE_URL + "/count";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("requestParam", requestParam.toArray());
        ResponseEntity<Result<List<ShortLinkGroupCountQueryRespDTO>>> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        return response.getBody();
    }
}
