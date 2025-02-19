package com.timecold.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timecold.shortlink.project.common.constant.RedisKeyConstant;
import com.timecold.shortlink.project.dao.entity.ShortLinkDO;
import com.timecold.shortlink.project.dao.mapper.ShortLinkMapper;
import com.timecold.shortlink.project.dto.req.RecycleBinArchiveReqDTO;
import com.timecold.shortlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements RecycleBinService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void archiveShortLink(RecycleBinArchiveReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getShortUri, requestParam.getShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(1)
                .build();
        baseMapper.update(shortLinkDO, queryWrapper);
        stringRedisTemplate.delete(RedisKeyConstant.SHORT_LINK_GOTO_KEY + requestParam.getShortUrl());
    }
}
