package com.timecold.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timecold.shortlink.project.common.constant.RedisKeyConstant;
import com.timecold.shortlink.project.common.constant.ShortLinkConstant;
import com.timecold.shortlink.project.common.convention.exception.ClientException;
import com.timecold.shortlink.project.common.convention.exception.ServiceException;
import com.timecold.shortlink.project.common.enums.VailDateTypeEnum;
import com.timecold.shortlink.project.dao.entity.ShortLinkDO;
import com.timecold.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.timecold.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.timecold.shortlink.project.dao.mapper.ShortLinkMapper;
import com.timecold.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.timecold.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.timecold.shortlink.project.service.ShortLinkService;
import com.timecold.shortlink.project.toolkit.HashUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;



/**
 * 短链接接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortLinkCachePenetrationBloomFilter;

    private final ShortLinkGotoMapper shortLinkGotoMapper;

    private final RedissonClient redissonClient;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        //TODO 数据校验
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = StrBuilder.create(requestParam.getDomain())
                .append("/")
                .append(shortLinkSuffix)
                .toString();
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(shortLinkSuffix)
                .enableStatus(0)
                .fullShortUrl(fullShortUrl)
                .build();
        ShortLinkGotoDO linkGotoDO  = ShortLinkGotoDO.builder()
                .gid(requestParam.getGid())
                .fullShortUrl(fullShortUrl)
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(linkGotoDO);
        } catch (DuplicateKeyException ex) {
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(shortLinkDO)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl);
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            if (hasShortLinkDO != null) {
                log.warn("短链接: {} 重复入库",fullShortUrl);
                throw new ServiceException("短链接生成重复");
            }
        }
        String gotoKey = RedisKeyConstant.SHORT_LINK_GOTO_KEY + shortLinkSuffix;
        updateUrlInfoCache(gotoKey, shortLinkDO);
        shortLinkCachePenetrationBloomFilter.add(shortLinkSuffix);
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid, count(*) as shortLinkCount")
                .in("gid", requestParam)
                .eq("enable_status", 0)
                .groupBy("gid");
        List<Map<String, Object>> shortLinkDOList = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(shortLinkDOList, ShortLinkGroupCountQueryRespDTO.class);
    }

    @Transactional
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接不存在");
        }
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(hasShortLinkDO.getDomain())
                .shortUri(hasShortLinkDO.getShortUri())
                .clickNumber(hasShortLinkDO.getClickNumber())
                .favicon(hasShortLinkDO.getFavicon())
                .createdType(hasShortLinkDO.getCreatedType())
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .describe(requestParam.getDescribe())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .build();
        if (Objects.equals(shortLinkDO.getGid(), requestParam.getOriginGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            baseMapper.update(shortLinkDO, updateWrapper);
        } else {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            baseMapper.delete(updateWrapper);
            baseMapper.insert(shortLinkDO);
        }
    }

    @SneakyThrows
    @Override
    public void redirectUrl(String shortUri, HttpServletRequest request, HttpServletResponse response) {
        String serverName = request.getServerName();
        String fullShortUrl = serverName + "/" + shortUri;
        String gotoKey = RedisKeyConstant.SHORT_LINK_GOTO_KEY + shortUri;
        if (!shortLinkCachePenetrationBloomFilter.contains(shortUri)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Map<Object, Object> urlInfo = stringRedisTemplate.opsForHash().entries(gotoKey);
        if (!urlInfo.isEmpty()) {
            long expireTimestamp = Long.parseLong((String) urlInfo.get("expireTimestamp"));
            long remaining = expireTimestamp - System.currentTimeMillis();
            if (expireTimestamp > 0 && remaining < 0) {
                stringRedisTemplate.delete(gotoKey);
            } else {
                response.sendRedirect((String)urlInfo.get("originUrl"));
                updateCacheTtl(gotoKey, expireTimestamp);
                return;
            }
        }
        if (isGotoNullCacheExists(shortUri)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        RLock lock = redissonClient.getLock(RedisKeyConstant.SHORT_LINK_GOTO_LOCK_KEY + fullShortUrl);
        try {
            lock.lock();
            if (isGotoNullCacheExists(shortUri)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            urlInfo = stringRedisTemplate.opsForHash().entries(gotoKey);
            if (!urlInfo.isEmpty()) {
                response.sendRedirect((String)urlInfo.get("originUrl"));
                return;
            }
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl));
            if (shortLinkGotoDO == null) {
                setGotoNullCache(shortUri);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            ShortLinkDO shortLinkDO = getValidShortLink(shortLinkGotoDO);
            if (shortLinkDO == null) {
                setGotoNullCache(shortUri);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.sendRedirect(shortLinkDO.getOriginUrl());
            updateUrlInfoCache(gotoKey, shortLinkDO);
        } finally {
            lock.unlock();
        }
    }

    //更新URL信息到缓存
    private void updateUrlInfoCache(String gotoKey, ShortLinkDO shortLinkDO) {
        Map<String, String> urlInfo = new HashMap<>();
        urlInfo.put("originUrl", shortLinkDO.getOriginUrl());
        long expireTimestamp = shortLinkDO.getValidDateType() == 0 ? 0 : shortLinkDO.getValidDate().getTime();
        urlInfo.put("expireTimestamp", String.valueOf(expireTimestamp));
        stringRedisTemplate.opsForHash().putAll(gotoKey, urlInfo);
        updateCacheTtl(gotoKey, expireTimestamp);
    }

    //查询有效ShortLink
    private ShortLinkDO getValidShortLink(ShortLinkGotoDO shortLinkGotoDO) {
        ShortLinkDO shortLinkDO = baseMapper.selectOne(Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                .eq(ShortLinkDO::getFullShortUrl, shortLinkGotoDO.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0));
        if (shortLinkDO == null) return null;
        if (shortLinkDO.getValidDateType() == 1 && shortLinkDO.getValidDate().before(new Date())) {
            return null;
        }
        return shortLinkDO;
    }

    //设置空值缓存
    private void setGotoNullCache(String shortUri) {
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstant.SHORT_LINK_GOTO_NULL_KEY + shortUri,
                "-", 30, TimeUnit.MINUTES
        );
    }

    //判断空值缓存是否存在
    private boolean isGotoNullCacheExists(String shortUri) {
        return stringRedisTemplate.hasKey(RedisKeyConstant.SHORT_LINK_GOTO_NULL_KEY + shortUri);
    }

    //更新缓存TTL
    private void updateCacheTtl(String gotoKey, long expireTimestamp) {
        long ttl = expireTimestamp == 0 ? ShortLinkConstant.DEFAULT_CACHE_TTL :
                Math.min(expireTimestamp - System.currentTimeMillis(), ShortLinkConstant.DEFAULT_CACHE_TTL);
        stringRedisTemplate.expire(gotoKey, ttl, TimeUnit.MILLISECONDS);
    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        int customGenerateCount = 0;
        String shortUri;
        while (true) {
            if (customGenerateCount > 10) {
                throw new ServiceException("短链接创建频繁，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            originUrl += UUID.randomUUID().toString();
            shortUri = HashUtil.hashToBase62(originUrl);
            if (!shortLinkCachePenetrationBloomFilter.contains(shortUri)) {
                break;
            }
            customGenerateCount++;
        }
        return shortUri;
    }
}



