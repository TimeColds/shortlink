package com.timecold.shortlink.admin.remote.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.admin.biz.user.UserContext;
import com.timecold.shortlink.admin.common.convention.exception.ServiceException;
import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.remote.dto.req.*;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.timecold.shortlink.admin.remote.service.RemoteShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RemoteShortLinkServiceImpl implements RemoteShortLinkService {

    private final RestTemplate restTemplate;

    public static final String REMOTE_URL = "http://localhost:8001/api/v1/short_link";

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String remoteUrl = REMOTE_URL + "/create";
        requestParam.setUid(UserContext.getUserId());
        Result<ShortLinkCreateRespDTO> result = restTemplate.exchange(remoteUrl,
                HttpMethod.POST,
                new HttpEntity<>(requestParam),
                new ParameterizedTypeReference<Result<ShortLinkCreateRespDTO>>() {
                }).getBody();
        if (result == null) {
            throw new ServiceException("创建短链失败");
        }
        return result.getData();
    }

    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        String remoteUrl = REMOTE_URL + "/update";
        requestParam.setUid(UserContext.getUserId());
        restTemplate.put(remoteUrl, requestParam);
    }

    @Override
    public Page<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        String remoteUrl = REMOTE_URL + "/page";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("gid", requestParam.getGid())
                .queryParam("size", requestParam.getSize())
                .queryParam("current", requestParam.getCurrent()).toUriString();
        Result<Page<ShortLinkPageRespDTO>> result = restTemplate.exchange(finalUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Result<Page<ShortLinkPageRespDTO>>>() {
                }).getBody();
        if (result == null) {
            throw new ServiceException("查询短链失败");
        }
        return result.getData();
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount() {
        String remoteUrl = REMOTE_URL + "/count";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("requestParam", UserContext.getUserId()).toUriString();
        Result<List<ShortLinkGroupCountQueryRespDTO>> result = restTemplate.exchange(
                finalUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Result<List<ShortLinkGroupCountQueryRespDTO>>>() {
                }
        ).getBody();
        if (result == null) {
            throw new ServiceException("查询短链接数量失败");
        }
        return result.getData();
    }

    @Override
    public String getTitleByUrl(String url) {
        String remoteUrl = REMOTE_URL + "/title";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("url", url).toUriString();
        Result<?> result = restTemplate.getForObject(finalUrl, Result.class);
        if (result == null) {
            throw new ServiceException("获取标题失败");
        }
        return (String) result.getData();
    }

    @Override
    public Page<ShortLinkPageRespDTO> pageArchivedShortLink(Long size, Long current) {
        String remoteUrl = REMOTE_URL + "/archived_links";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("size", size)
                .queryParam("current", current)
                .queryParam("uid", UserContext.getUserId()).toUriString();
        Result<Page<ShortLinkPageRespDTO>> result = restTemplate.exchange(finalUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Result<Page<ShortLinkPageRespDTO>>>() {
                }).getBody();
        if (result == null) {
            throw new ServiceException("查询归档链接失败");
        }
        return result.getData();
    }

    @Override
    public void archiveShortLink(ArchiveReqDTO requestParam) {
        String remoteUrl = REMOTE_URL + "/archive";
        requestParam.setUid(UserContext.getUserId());
        restTemplate.put(remoteUrl, requestParam);
    }
    @Override
    public void recoverShortLink(ArchiveRecoverReqDTO requestParam) {
        String remoteUrl = REMOTE_URL + "/archive_recover";
        requestParam.setUid(UserContext.getUserId());
        restTemplate.put(remoteUrl, requestParam);
    }

    @Override
    public void archiveRemove(ArchiveRemoveReqDTO requestParam) {
        String remoteUrl = REMOTE_URL + "/delete";
        requestParam.setUid(UserContext.getUserId());
        restTemplate.put(remoteUrl, requestParam);
    }

}
