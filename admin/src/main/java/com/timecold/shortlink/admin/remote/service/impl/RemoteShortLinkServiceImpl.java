package com.timecold.shortlink.admin.remote.service.impl;

import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.remote.dto.req.RecycleBinArchiveReqDTO;
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
        String remoteUrl = REMOTE_URL + "/create";
        return restTemplate.postForObject(remoteUrl, requestParam, Result.class);
    }

    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        String remoteUrl = REMOTE_URL + "/update";
        restTemplate.put(remoteUrl, requestParam);
    }

    @Override
    public Result pageShortLink(ShortLinkPageReqDTO requestParam) {
        String remoteUrl = REMOTE_URL + "/page";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("gid", requestParam.getGid())  // 添加其他参数
                .queryParam("size", requestParam.getSize())
                .queryParam("current", requestParam.getCurrent());
        String finalUrl = builder.toUriString();
        return restTemplate.getForObject(finalUrl, Result.class);
    }

    @Override
    public Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(List<String> requestParam) {
        String remoteUrl = REMOTE_URL + "/count";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("requestParam", requestParam.toArray()).toUriString();
        ResponseEntity<Result<List<ShortLinkGroupCountQueryRespDTO>>> response = restTemplate.exchange(
                finalUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        return response.getBody();
    }

    @Override
    public String getTitleByUrl(String url) {
        String remoteUrl = REMOTE_URL + "/title";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("url", url).toUriString();
        return restTemplate.getForObject(finalUrl, String.class);
    }

    @Override
    public void archiveShortLink(RecycleBinArchiveReqDTO requestParam) {
        String remoteUrl = REMOTE_URL + "/archive";
        restTemplate.put(remoteUrl, requestParam);
    }
}
