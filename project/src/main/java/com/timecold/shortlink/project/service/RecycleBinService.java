package com.timecold.shortlink.project.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.timecold.shortlink.project.dao.entity.ShortLinkDO;
import com.timecold.shortlink.project.dto.req.RecycleBinArchiveReqDTO;

/**
 * 回收站管理接口层
 */
public interface RecycleBinService extends IService<ShortLinkDO> {


    /**
     *    归档短链接
     *
     * @param requestParam 归档短链接请求参数
     */
    void archiveShortLink(RecycleBinArchiveReqDTO requestParam);
}
