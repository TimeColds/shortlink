package com.timecold.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timecold.shortlink.project.common.constant.RedisKeyConstant;
import com.timecold.shortlink.project.common.convention.exception.ClientException;
import com.timecold.shortlink.project.dao.entity.ShortLinkDO;
import com.timecold.shortlink.project.dao.mapper.ShortLinkMapper;
import com.timecold.shortlink.project.dto.req.ArchiveRecoverDTO;
import com.timecold.shortlink.project.dto.req.ArchiveReqDTO;
import com.timecold.shortlink.project.dto.req.ArchivedPageReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.timecold.shortlink.project.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ArchiveService {

    private final StringRedisTemplate stringRedisTemplate;

    private final RBloomFilter<String> shortUrlCachePenetrationBloomFilter;

    @Override
    public void archiveShortLink(ArchiveReqDTO requestParam) {
        if (!shortUrlCachePenetrationBloomFilter.contains(requestParam.getShortUrl())) {
            throw new ClientException("短链不存在");
        }
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getShortUrl, requestParam.getShortUrl())
                .eq(ShortLinkDO::getUid, requestParam.getUid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .set(ShortLinkDO::getEnableStatus, 1);
        ShortLinkDO shortLinkDO = new ShortLinkDO();
        int updateRow = baseMapper.update(shortLinkDO, updateWrapper);
        if (updateRow == 0) {
            throw new ClientException("归档失败");
        }
        stringRedisTemplate.delete(RedisKeyConstant.SHORT_LINK_GOTO_KEY + requestParam.getShortUrl());
    }

    @Override
    public Page<ShortLinkPageRespDTO> pageArchivedShortLink(ArchivedPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getUid, requestParam.getUid())
                .eq(ShortLinkDO::getEnableStatus, 1)
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
    public void recoverShortLink(ArchiveRecoverDTO requestParam) {
        if (!shortUrlCachePenetrationBloomFilter.contains(requestParam.getShortUrl())) {
            throw new ClientException("短链不存在");
        }
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getShortUrl, requestParam.getShortUrl())
                .eq(ShortLinkDO::getUid, requestParam.getUid())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0)
                .set(ShortLinkDO::getEnableStatus, 0)
                .set(ShortLinkDO::getGid, requestParam.getGid());
        ShortLinkDO shortLinkDO = new ShortLinkDO();
        int updateRow = baseMapper.update(shortLinkDO, updateWrapper);
        if (updateRow == 0) {
            throw new ClientException("恢复失败");
        }
    }
}
