package com.timecold.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.net.InetAddresses;
import com.timecold.shortlink.project.common.constant.RedisKeyConstant;
import com.timecold.shortlink.project.common.constant.ShortLinkConstant;
import com.timecold.shortlink.project.common.convention.exception.ClientException;
import com.timecold.shortlink.project.common.convention.exception.ServiceException;
import com.timecold.shortlink.project.dao.entity.ShortLinkDO;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


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

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String shortUrl = generateShortUrl(requestParam);
        String fullShortUrl = requestParam.getDomain() + "/" + shortUrl;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .favicon(getFavicon(requestParam.getOriginUrl()))
                .shortUrl(shortUrl)
                .uid(requestParam.getUid())
                .createdType(1)
                .fullShortUrl(fullShortUrl)
                .enableStatus(0)
                .build();
        BeanUtils.copyProperties(requestParam, shortLinkDO);
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
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getUpdateTime);
        Page<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return (Page<ShortLinkPageRespDTO>) resultPage.convert(each -> {
            ShortLinkPageRespDTO shortLinkPageRespDTO = new ShortLinkPageRespDTO();
            BeanUtils.copyProperties(each, shortLinkPageRespDTO);
            shortLinkPageRespDTO.setDomain("http://" + each.getDomain());
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
        updateUrlInfoCache(RedisKeyConstant.LINK_GOTO_KEY + requestParam.getShortUrl(), requestParam.getOriginUrl(), requestParam.getValidDate());
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
        long ttl = expireTimestamp == 0 ? ShortLinkConstant.DEFAULT_CACHE_TTL :
                Math.min(expireTimestamp - System.currentTimeMillis(), ShortLinkConstant.DEFAULT_CACHE_TTL);
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



