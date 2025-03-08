package com.timecold.shortlink.admin.remote.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.admin.biz.user.UserContext;
import com.timecold.shortlink.admin.common.convention.exception.ServiceException;
import com.timecold.shortlink.admin.remote.dto.req.*;
import com.timecold.shortlink.admin.remote.dto.resp.*;
import com.timecold.shortlink.admin.remote.service.RemoteShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
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
        ShortLinkCreateRespDTO result = restTemplate.exchange(remoteUrl,
                HttpMethod.POST,
                new HttpEntity<>(requestParam),
                ShortLinkCreateRespDTO.class
                ).getBody();
        if (result == null) {
            throw new ServiceException("创建短链失败");
        }
        return result;
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
        Page<ShortLinkPageRespDTO> result = restTemplate.exchange(finalUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<ShortLinkPageRespDTO>>() {
                }).getBody();
        if (result == null) {
            throw new ServiceException("查询短链失败");
        }
        return result;
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount() {
        String remoteUrl = REMOTE_URL + "/count";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("requestParam", UserContext.getUserId()).toUriString();
        List<ShortLinkGroupCountQueryRespDTO> result = restTemplate.exchange(
                finalUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ShortLinkGroupCountQueryRespDTO>>() {
                }
        ).getBody();
        if (result == null) {
            throw new ServiceException("查询短链接数量失败");
        }
        return result;
    }

    @Override
    public String getTitleByUrl(String url) {
        String remoteUrl = REMOTE_URL + "/title";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("url", url).toUriString();
        String result = restTemplate.getForObject(finalUrl, String.class);
        if (result == null) {
            throw new ServiceException("获取标题失败");
        }
        return result;
    }

    @Override
    public Page<ShortLinkPageRespDTO> pageArchivedShortLink(Long size, Long current) {
        String remoteUrl = REMOTE_URL + "/archived_links";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("size", size)
                .queryParam("current", current)
                .queryParam("uid", UserContext.getUserId()).toUriString();
        Page<ShortLinkPageRespDTO> result = restTemplate.exchange(finalUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<ShortLinkPageRespDTO>>() {
                }).getBody();
        if (result == null) {
            throw new ServiceException("查询归档链接失败");
        }
        return result;
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

    @Override
    public ShortLinkDailyStatsRespDTO getDailyStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        String remoteUrl = REMOTE_URL + "/stats/daily_stats";

        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("shortUrl", shortUrl)
                .queryParam("beginDate", beginDate)
                .queryParam("endDate", endDate).toUriString();
        ShortLinkDailyStatsRespDTO result = restTemplate.getForObject(finalUrl, ShortLinkDailyStatsRespDTO.class);
        if (result == null) {
            throw new ServiceException("查询短链失败");
        }
        return result;
    }

    @Override
    public ShortLinkChartStatsRespDTO getChartStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        String remoteUrl = REMOTE_URL + "/stats/chart_stats";

        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("shortUrl", shortUrl)
                .queryParam("beginDate", beginDate)
                .queryParam("endDate", endDate).toUriString();
        ShortLinkChartStatsRespDTO result = restTemplate.getForObject(finalUrl, ShortLinkChartStatsRespDTO.class);
        if (result == null) {
            throw new ServiceException("查询短链失败");
        }
        return result;
    }

    @Override
    public Page<ShortLinkLogPageRespDTO> getLogStats(ShortLinkLogPageReqDTO requestParam) {
        String remoteUrl = REMOTE_URL + "/stats/log_stats";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(remoteUrl)
                .queryParam("shortUrl", requestParam.getShortUrl())
                .queryParam("beginDate", requestParam.getBeginDate())
                .queryParam("endDate", requestParam.getEndDate())
                .queryParam("size", requestParam.getSize())
                .queryParam("current", requestParam.getCurrent()).toUriString();
        Page<ShortLinkLogPageRespDTO> result = restTemplate.exchange(finalUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<ShortLinkLogPageRespDTO>>() {
                }).getBody();
        if (result == null) {
            throw new ServiceException("查询短链失败");
        }
        return result;
    }

}
