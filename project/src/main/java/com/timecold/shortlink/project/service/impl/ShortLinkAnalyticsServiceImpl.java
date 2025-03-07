package com.timecold.shortlink.project.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecold.shortlink.project.common.convention.exception.ServiceException;
import com.timecold.shortlink.project.dao.entity.LinkLogStatsDO;
import com.timecold.shortlink.project.dao.mapper.LinkLogStatsMapper;
import com.timecold.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.timecold.shortlink.project.service.ShortLinkAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static com.timecold.shortlink.project.common.constant.RedisKeyConstant.*;
import static com.timecold.shortlink.project.common.constant.RedisStreamConstant.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkAnalyticsServiceImpl implements ShortLinkAnalyticsService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserAgentAnalyzer userAgentAnalyzer;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LinkLogStatsMapper linkLogStatsMapper;

    @Value("${gaode.api-key}")
    private String amapKey;

    @Override
    @Async("statsThreadPool")
    public void processAccess(ShortLinkStatsRecordDTO requestParam) {
        UserAgent userAgent = userAgentAnalyzer.parse(requestParam.getUserAgent());
        String device = userAgent.getValue(UserAgent.DEVICE_CLASS);
        String os = userAgent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        String browser = userAgent.getValue(UserAgent.AGENT_NAME);
        String userIdentifier = requestParam.getUserIdentifier();
        String ip = requestParam.getIp();
        String location = obtainLocation(ip);
        LocalDateTime accessTime = requestParam.getAccessTime();
        String year = String.valueOf(accessTime.getYear());
        String dateStr = accessTime.toLocalDate().toString();
        String hour = String.valueOf(accessTime.getHour());
        String shortUrl = requestParam.getShortUrl();

        LinkLogStatsDO linkLogStatsDO = LinkLogStatsDO.builder()
                .shortUrl(shortUrl)
                .accessTime(accessTime)
                .ip(ip)
                .referer(requestParam.getReferer())
                .device(device)
                .os(os)
                .browser(browser)
                .location(location)
                .build();

        ZoneOffset zoneOffset = ZoneOffset.ofHours(8);
        //pv统计
        String pvKey = LINK_PV_KEY_PREFIX + shortUrl + ":" + dateStr;
        stringRedisTemplate.opsForHash().increment(pvKey, hour, 1);
        stringRedisTemplate.opsForHash().increment(pvKey, "total", 1);
        stringRedisTemplate.expireAt(pvKey, accessTime.plusDays(1).toInstant(zoneOffset));
        //uv统计
        String uvKey = LINK_UV_KEY_PREFIX + shortUrl + ":" + dateStr;
        stringRedisTemplate.opsForHyperLogLog().add(uvKey, userIdentifier);

        stringRedisTemplate.expireAt(uvKey, accessTime.plusDays(1).toInstant(zoneOffset));
        //uv签到统计
        String uvTypeKey = LINK_UV_KEY_PREFIX + shortUrl + ":" + year + ":" + userIdentifier;
        Long uvType = stringRedisTemplate.execute((RedisCallback<Long>) connection ->
                connection.stringCommands().bitPos(uvTypeKey.getBytes(), true, Range.unbounded())
        );
        if (uvType == -1) {
            linkLogStatsDO.setVisitorType(0);
        } else {
            linkLogStatsDO.setVisitorType(1);
        }
        stringRedisTemplate.opsForValue().setBit(uvTypeKey,accessTime.getDayOfYear(), true);
        //ip统计
        String uipKey = LINK_UIP_KEY_PREFIX + shortUrl + ":" + dateStr;
        stringRedisTemplate.opsForHyperLogLog().add(uipKey , ip);
        stringRedisTemplate.expireAt(uipKey, accessTime.plusDays(1).toInstant(zoneOffset));

        //平台统计
        String platformStatsKey = LINK_PLATFORM_KEY_PREFIX + shortUrl + ":" + dateStr;
        String platform = device + ":" + os + ":" + browser;
        stringRedisTemplate.opsForHash().increment(platformStatsKey, platform, 1);
        stringRedisTemplate.expireAt(platformStatsKey, accessTime.plusDays(1).toInstant(zoneOffset));

        //位置统计
        String locationStatsKey = LINK_LOCATION_KEY_PREFIX + shortUrl + ":" + dateStr;
        stringRedisTemplate.opsForHash().increment(locationStatsKey, location, 1);
        stringRedisTemplate.expireAt(locationStatsKey, accessTime.plusDays(1).toInstant(zoneOffset));

        Map<String, Object> dailyStatsEvent = new HashMap<>();
        dailyStatsEvent.put("shortUrl", shortUrl);
        dailyStatsEvent.put("date", dateStr);
        dailyStatsEvent.put("hour", hour);
        dailyStatsEvent.put("userIdentifier", userIdentifier);
        stringRedisTemplate.opsForStream().add(DAILY_STATS_STREAM, dailyStatsEvent);

        Map<String, Object> platformStatsEvent = new HashMap<>();
        platformStatsEvent.put("shortUrl", shortUrl);
        platformStatsEvent.put("date", dateStr);
        platformStatsEvent.put("device", device);
        platformStatsEvent.put("os", os);
        platformStatsEvent.put("browser", browser);
        stringRedisTemplate.opsForStream().add(PLATFORM_STATS_STREAM, platformStatsEvent);

        Map<String, Object> locationStatsEvent = new HashMap<>();
        locationStatsEvent.put("shortUrl", shortUrl);
        locationStatsEvent.put("date", dateStr);
        locationStatsEvent.put("province", location);
        stringRedisTemplate.opsForStream().add(LOCATION_STATS_STREAM, locationStatsEvent);

        String linkStatsLog;
        try {
            linkStatsLog = objectMapper.writeValueAsString(linkLogStatsDO);
        } catch (JsonProcessingException e) {
            throw new ServiceException("json序列化失败");
        }
        Map<String, Object> statsLogEvent = new HashMap<>();
        statsLogEvent.put("linkStatsLog", linkStatsLog);
        stringRedisTemplate.opsForStream().add(LOG_STATS_STREAM, statsLogEvent);
    }

    private void processAccessLog() {

    }

    private void processDailyStats() {

    }

    private void processDeviceStats() {

    }

    private void processLocationStats() {

    }

    private String obtainLocation(String ip) {
//        String amapApi = "https://restapi.amap.com/v3/ip";
//        String finalUrl = UriComponentsBuilder.fromHttpUrl(amapApi)
//                .queryParam("key", amapKey)
//                .queryParam("ip", ip).build().toUriString();
//        String locationJson = restTemplate.getForObject(finalUrl, String.class);
//        try {
//            Map<String, String> map = objectMapper.readValue(locationJson, new TypeReference<>() {});
//            String infoCode = map.get("infocode");
//            if (infoCode.equals("10000")) {
//                return "中国" + map.get("province") + map.get("city");
//            } else {
//                return null;
//            }
//        } catch (JsonProcessingException e) {
//            log.error("获取地理位置失败", e);
//        }
        return "中国";
    }
}
