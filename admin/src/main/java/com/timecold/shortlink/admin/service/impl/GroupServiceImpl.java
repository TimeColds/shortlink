package com.timecold.shortlink.admin.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timecold.shortlink.admin.biz.user.UserContext;
import com.timecold.shortlink.admin.common.convention.exception.ClientException;
import com.timecold.shortlink.admin.dao.entity.GroupDO;
import com.timecold.shortlink.admin.dao.mapper.GroupMapper;
import com.timecold.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.timecold.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.timecold.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.timecold.shortlink.admin.remote.service.RemoteShortLinkService;
import com.timecold.shortlink.admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 短链接分组接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    private final RemoteShortLinkService remoteShortLinkService;

    @Override
    public void saveGroup(String groupName) {
        saveGroup(UserContext.getUserId(), groupName);
    }

    @Override
    public void saveGroup(Long uid, String groupName) {
        Long gid = IdUtil.getSnowflakeNextId();
        GroupDO groupDO = GroupDO.builder()
                .gid(gid)
                .name(groupName)
                .uid(uid)
                .sortOrder(0)
                .build();
        baseMapper.insert(groupDO);
    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUid, UserContext.getUserId())
                .eq(GroupDO::getDelFlag, 0)
                .orderByDesc(Arrays.asList(GroupDO::getSortOrder, GroupDO::getUpdateTime));
        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
        List<ShortLinkGroupCountQueryRespDTO> countList = remoteShortLinkService.listGroupShortLinkCount();
        Map<Long, Long> gidCountMap = countList.stream().collect(Collectors.toMap(
                ShortLinkGroupCountQueryRespDTO::getGid,
                ShortLinkGroupCountQueryRespDTO::getShortLinkCount));
        return groupDOList.stream().map(groupDO -> {
            ShortLinkGroupRespDTO shortLinkGroupRespDTO = new ShortLinkGroupRespDTO();
            BeanUtils.copyProperties(groupDO, shortLinkGroupRespDTO);
            shortLinkGroupRespDTO.setShortLinkCount(gidCountMap.getOrDefault(groupDO.getGid(), 0L));
            return shortLinkGroupRespDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUid, UserContext.getUserId())
                .eq(GroupDO::getGid, requestParam.getGid())
                .eq(GroupDO::getDelFlag, 0)
                .set(GroupDO::getName, requestParam.getName());
        GroupDO groupDO = new GroupDO();
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void deleteGroup(Long gid) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUid, UserContext.getUserId())
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0)
                .set(GroupDO::getDelFlag, 1);
        int updateRow = baseMapper.update(null, updateWrapper);
        if (updateRow == 0) {
            throw new RuntimeException("删除失败");
        }
    }

    @Override
    public void sortGroup(ShortLinkGroupSortReqDTO requestParam) {
        List<Long> gidList = requestParam.getGidList();
        if (CollectionUtils.isEmpty(gidList)) {
            throw new ClientException("排序列表为空");
        }
        for (int i = 0; i < gidList.size(); i++) {
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getUid, UserContext.getUserId())
                    .eq(GroupDO::getGid, gidList.get(i))
                    .eq(GroupDO::getDelFlag, 0)
                    .set(GroupDO::getSortOrder, i);
            baseMapper.update(null, updateWrapper);
        }
    }
}
