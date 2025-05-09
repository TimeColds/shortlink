package com.timecold.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.timecold.shortlink.admin.dao.entity.GroupDO;
import com.timecold.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.timecold.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.timecold.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;

import java.util.List;

/**
 * 短链接分组接口层
 */
public interface GroupService extends IService<GroupDO> {

    /**
     * 新增短链接分组
     * @param groupName 短链接分组名
     */
    void saveGroup(String groupName);


    /**
     * 新增短链接分组
     *
     * @param uid  用户id
     * @param groupName 短链接分组名
     */
    void saveGroup(Long uid, String groupName);

    /**
     * 查询用户短链接分组集合
     * @return 用户短链接分组集合
     */
    List<ShortLinkGroupRespDTO> listGroup();

    /**
     * 修改短链接分组
     *
     * @param requestParam 修改链接分组参数
     */
    void updateGroup(ShortLinkGroupUpdateReqDTO requestParam);

    /**
     * 删除短链接分组
     *
     * @param gid 删除短链接分组的分组标识
     */
    void deleteGroup(Long gid);

    /**
     * 短链接分组排序
     *
     * @param requestParam 短链接分组排序参数
     */
    void sortGroup(ShortLinkGroupSortReqDTO requestParam);
}
