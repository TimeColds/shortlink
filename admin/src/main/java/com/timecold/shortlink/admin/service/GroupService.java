package com.timecold.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.timecold.shortlink.admin.dao.entity.GroupDO;
import com.timecold.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;

import java.util.List;

/**
 * 短链接分组接口层
 */
public interface GroupService extends IService<GroupDO> {

    /**
     * 新增短链接分组
     * @param name 短链接分组名
     */
    void saveGroup(String name);

    /**
     * 查询用户短链接分组集合
     * @return 用户短链接分组集合
     */
    List<ShortLinkGroupRespDTO> listGroup();
}
