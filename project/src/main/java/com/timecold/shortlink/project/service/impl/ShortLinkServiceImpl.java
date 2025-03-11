package com.timecold.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.net.InetAddresses;
import com.timecold.shortlink.project.common.constant.RedisKeyConstant;
import com.timecold.shortlink.project.common.convention.exception.ClientException;
import com.timecold.shortlink.project.common.convention.exception.ServiceException;
import com.timecold.shortlink.project.dao.entity.LinkDailyStatsDO;
import com.timecold.shortlink.project.dao.entity.ShortLinkDO;
import com.timecold.shortlink.project.dao.mapper.LinkDailyStatsMapper;
import com.timecold.shortlink.project.dao.mapper.ShortLinkMapper;
import com.timecold.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.timecold.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.timecold.shortlink.project.service.ShortLinkService;
import com.timecold.shortlink.project.toolkit.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.timecold.shortlink.project.common.constant.RedisKeyConstant.*;


/**
 * 短链接接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUrlCachePenetrationBloomFilter;

    private final RedissonClient redissonClient;

    private final StringRedisTemplate stringRedisTemplate;

    private final RestTemplate restTemplate;
    private final LinkDailyStatsMapper linkDailyStatsMapper;

    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;


    public static final long DEFAULT_CACHE_TTL = 24 * 60 * 60 * 1000;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String shortUrl = generateShortUrl(requestParam);
        String fullShortUrl = createShortLinkDefaultDomain + ":8001/" + shortUrl;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(createShortLinkDefaultDomain)
                .favicon(getFavicon(requestParam.getOriginUrl()))
                .shortUrl(shortUrl)
                .uid(requestParam.getUid())
                .createdType(1)
                .fullShortUrl(fullShortUrl)
                .enableStatus(0)
                .build();
        BeanUtils.copyProperties(requestParam, shortLinkDO, "domain");
        try {
            baseMapper.insert(shortLinkDO);
        } catch (DuplicateKeyException ex) {
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(shortLinkDO)
                    .select(ShortLinkDO::getShortUrl)
                    .eq(ShortLinkDO::getShortUrl, shortUrl);
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            if (hasShortLinkDO != null) {
                log.warn("短链接: {} 重复入库", fullShortUrl);
                throw new ServiceException("短链接生成重复");
            }
        }
        String gotoKey = RedisKeyConstant.LINK_GOTO_KEY + shortUrl;
        updateUrlInfoCache(gotoKey, shortLinkDO.getOriginUrl(), shortLinkDO.getValidDate());
        shortUrlCachePenetrationBloomFilter.add(shortUrl);
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public Page<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getUid, requestParam.getUid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getUpdateTime);
        LocalDate now = LocalDate.now();
        Page<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        if (resultPage.getRecords().isEmpty()) {
            return new Page<>();
        }
        List<ShortLinkDO> records = resultPage.getRecords();
        List<String> shortUrls = records.stream()
                .map(ShortLinkDO::getShortUrl)
                .collect(Collectors.toList());
        QueryWrapper<LinkDailyStatsDO> linkDailyStatsDOQueryWrapper = Wrappers.query(LinkDailyStatsDO.class)
                .select("short_url, " +
                        "SUM(pv) AS histPv," +
                        "SUM(uv) AS histUv," +
                        "SUM(uip) AS histUip")
                .in("short_url", shortUrls)
                .eq("del_flag", 0)
                .lt("stats_date", now)
                .groupBy("short_url");
        List<Map<String, Object>> list = linkDailyStatsMapper.selectMaps(linkDailyStatsDOQueryWrapper);
        Map<String, Map<String, Object>> shortUrlStatsMap = new HashMap<>();
        for (Map<String, Object> item : list) {
            String shortUrl = (String) item.get("short_url");
            shortUrlStatsMap.put(shortUrl, item);
        }
        Map<String, ShortLinkPageRespDTO.todayStats> todayStatsMap = new HashMap<>();
        Map<String, ShortLinkPageRespDTO.allStats> allStatsMap = new HashMap<>();
        for (ShortLinkDO record : records) {
            String shortUrl = record.getShortUrl();

            Long histPv = 0L;
            Long histUv = 0L;
            Long histUip = 0L;

            if (shortUrlStatsMap.containsKey(shortUrl)) {
                Map<String, Object> statsData = shortUrlStatsMap.get(shortUrl);
                histPv = Long.parseLong(statsData.get("histPv").toString());
                histUv = Long.parseLong(statsData.get("histUv").toString());
                histUip = Long.parseLong(statsData.get("histUip").toString());
            }

            String pvKey = LINK_PV_KEY_PREFIX + shortUrl + ":" + now;
            String uvKey = LINK_UV_KEY_PREFIX + shortUrl + ":" + now;
            String uipKey = LINK_UIP_KEY_PREFIX + shortUrl + ":" + now;
            Object pvResult = stringRedisTemplate.opsForHash().get(pvKey, "total");
            Long todayPv = pvResult != null ? Long.parseLong(pvResult.toString()) : 0L;
            Long todayUv = stringRedisTemplate.opsForHyperLogLog().size(uvKey);
            Long todayUip = stringRedisTemplate.opsForHyperLogLog().size(uipKey);

            todayStatsMap.put(shortUrl, new ShortLinkPageRespDTO.todayStats(todayPv, todayUv, todayUip));
            allStatsMap.put(shortUrl, new ShortLinkPageRespDTO.allStats(histPv + todayPv, histUv + todayUv, histUip + todayUip));
        }
        return (Page<ShortLinkPageRespDTO>) resultPage.convert(record -> {
            String shortUrl = record.getShortUrl();
            ShortLinkPageRespDTO shortLinkPageRespDTO = new ShortLinkPageRespDTO();
            BeanUtils.copyProperties(record, shortLinkPageRespDTO);
            shortLinkPageRespDTO.setDomain("http://" + record.getDomain());
            ShortLinkPageRespDTO.todayStats todayStats = todayStatsMap.get(shortUrl);
            shortLinkPageRespDTO.setTodayStats(todayStats);
            ShortLinkPageRespDTO.allStats allStats = allStatsMap.get(shortUrl);
            shortLinkPageRespDTO.setAllStats(allStats);
            return shortLinkPageRespDTO;
        });
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(Long uid) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid, count(*) as shortLinkCount")
                .eq("uid", uid)
                .eq("enable_status", 0)
                .groupBy("gid");
        List<Map<String, Object>> shortLinkDOList = baseMapper.selectMaps(queryWrapper);
        return shortLinkDOList.stream().map(map -> new ShortLinkGroupCountQueryRespDTO(
                (Long) map.get("gid"),
                (Long) map.get("shortLinkCount")
        )).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        if (!shortUrlCachePenetrationBloomFilter.contains(requestParam.getShortUrl())) {
            throw new ClientException("短链不存在");
        }
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getShortUrl, requestParam.getShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getUid, requestParam.getUid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .set(ShortLinkDO::getValidDate, requestParam.getValidDate())
                .set(requestParam.getOriginUrl() != null, ShortLinkDO::getOriginUrl, requestParam.getOriginUrl())
                .set(!requestParam.getGid().equals(requestParam.getOriginGid()), ShortLinkDO::getGid, requestParam.getGid())
                .set(requestParam.getDescribe() != null, ShortLinkDO::getDescribe, requestParam.getDescribe())
                .set(requestParam.getOriginUrl() != null, ShortLinkDO::getFavicon, getFavicon(requestParam.getOriginUrl()));
        ShortLinkDO shortLinkDO = new ShortLinkDO();
        int updateRow = baseMapper.update(shortLinkDO, updateWrapper);
        if (updateRow == 0) {
            throw new ClientException("更新失败");
        }
        if(!Objects.equals(requestParam.getOriginGid(), requestParam.getGid())
        || !Objects.equals(requestParam.getOriginUrl(), requestParam.getOriginUrl())
        || !Objects.equals(requestParam.getValidDate(), requestParam.getValidDate())) {
            deleteUrlCache(requestParam.getShortUrl());
        }

    }

    @Override
    public String redirectUrl(String shortUrl) {
        String gotoKey = RedisKeyConstant.LINK_GOTO_KEY + shortUrl;
        if (!shortUrlCachePenetrationBloomFilter.contains(shortUrl)) {
            return "notFound";
        }
        Map<Object, Object> urlInfo = stringRedisTemplate.opsForHash().entries(gotoKey);
        if (!urlInfo.isEmpty()) {
            long expireTimestamp = Long.parseLong((String) urlInfo.get("expireTimestamp"));
            long remaining = expireTimestamp - System.currentTimeMillis();
            if (expireTimestamp > 0 && remaining < 0) {
                stringRedisTemplate.delete(gotoKey);
                return "notFound";
            } else {
                updateCacheTtl(gotoKey, expireTimestamp);
                return (String) urlInfo.get("originUrl");
            }
        }
        if (isGotoNullCacheExists(shortUrl)) {
            return "notFound";
        }
        RLock lock = redissonClient.getLock(RedisKeyConstant.LINK_GOTO_LOCK_KEY + shortUrl);
        try {
            lock.lock();
            if (isGotoNullCacheExists(shortUrl)) {
                return "notFound";
            }
            urlInfo = stringRedisTemplate.opsForHash().entries(gotoKey);
            if (!urlInfo.isEmpty()) {
                return (String) urlInfo.get("originUrl");
            }
            ShortLinkDO shortLinkDO = getValidShortLink(shortUrl);
            if (shortLinkDO == null) {
                stringRedisTemplate.opsForValue().set(
                        RedisKeyConstant.LINK_GOTO_NULL_KEY + shortUrl,
                        "-", 30, TimeUnit.MINUTES);
                return "notFound";
            } else {
                updateUrlInfoCache(gotoKey, shortLinkDO.getOriginUrl(), shortLinkDO.getValidDate());
                return shortLinkDO.getOriginUrl();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getTitleByUrl(String url) {
        String html = fetchHtml(url);
        if (html != null) {
            Document doc = Jsoup.parse(html);
            return doc.title();
        } else {
            return "";
        }
    }

    private ShortLinkDO getValidShortLink(String shortUrl) {
        ShortLinkDO shortLinkDO = baseMapper.selectOne(Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getShortUrl, shortUrl)
                .eq(ShortLinkDO::getDelFlag, 0));
        if (shortLinkDO == null) {
            return null;
        }
        if (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())) {
            return null;
        }
        return shortLinkDO;
    }

    /**
     * 删除短链接相关的缓存
     * @param shortUrl 短链接URL
     */
    private void deleteUrlCache(String shortUrl) {
        try {
            String gotoKey = RedisKeyConstant.LINK_GOTO_KEY + shortUrl;
            stringRedisTemplate.delete(gotoKey);
            log.info("短链接缓存删除成功, shortUrl: {}", shortUrl);
        } catch (Exception e) {
            log.error("短链接缓存删除失败, shortUrl: {}, 原因: {}", shortUrl, e.getMessage(), e);
        }
    }

    private void updateUrlInfoCache(String gotoKey, String originUrl, Date vailDate) {
        Map<String, String> urlInfo = new HashMap<>();
        urlInfo.put("originUrl", originUrl);
        long expireTimestamp = vailDate == null ? 0 : vailDate.getTime();
        urlInfo.put("expireTimestamp", String.valueOf(expireTimestamp));
        stringRedisTemplate.opsForHash().putAll(gotoKey, urlInfo);
        updateCacheTtl(gotoKey, expireTimestamp);
    }

    private boolean isGotoNullCacheExists(String shortUrl) {
        return stringRedisTemplate.hasKey(RedisKeyConstant.LINK_GOTO_NULL_KEY + shortUrl);
    }

    private void updateCacheTtl(String gotoKey, long expireTimestamp) {
        long ttl = expireTimestamp == 0 ? DEFAULT_CACHE_TTL :
                Math.min(expireTimestamp - System.currentTimeMillis(), DEFAULT_CACHE_TTL);
        stringRedisTemplate.expire(gotoKey, ttl, TimeUnit.MILLISECONDS);
    }

    private String generateShortUrl(ShortLinkCreateReqDTO requestParam) {
        int customGenerateCount = 0;
        String shortUrl;
        while (true) {
            if (customGenerateCount > 10) {
                throw new ServiceException("短链接创建频繁，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            originUrl += UUID.randomUUID().toString();
            shortUrl = HashUtil.hashToBase62(originUrl);
            if (!shortUrlCachePenetrationBloomFilter.contains(shortUrl)) {
                break;
            }
            customGenerateCount++;
        }
        return shortUrl;
    }

    private String getFavicon(String originUrl) {
        String html = fetchHtml(originUrl);
        Document doc = Jsoup.parse(html, originUrl);
        Elements elements = doc.select("link[rel~=icon]");
        if (!elements.isEmpty()) {
            return Objects.requireNonNull(elements.first()).absUrl("href");
        } else {
            return URI.create(originUrl).resolve("/favicon.ico").toString();
        }
    }

    private String fetchHtml(String originUrl) {
        URI uri = URI.create(originUrl);
        String host = uri.getHost();
        if (InetAddresses.isInetAddress(host)) {
            throw new ClientException("不支持的URL格式");
        } else {
            return restTemplate.getForObject(originUrl, String.class);
        }
    }
}



