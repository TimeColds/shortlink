package com.timecold.shortlink.project.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timecold.shortlink.project.dao.entity.ShortLinkDO;
import com.timecold.shortlink.project.dto.req.ArchiveRecoverDTO;
import com.timecold.shortlink.project.dto.req.ArchiveReqDTO;
import com.timecold.shortlink.project.dto.req.ArchivedPageReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkPageRespDTO;

/**
 * 回收站管理接口层
 */
public interface ArchiveService extends IService<ShortLinkDO> {


    /**
     *    归档短链接
     *
     * @param requestParam 归档短链接请求参数
     */
    void archiveShortLink(ArchiveReqDTO requestParam);

    /**
     * 分页查询归档的短链接
     * @param requestParam 分页查询归档的短链接请求参数
     * @return 归档短链接分页响应
     */
    Page<ShortLinkPageRespDTO> pageArchivedShortLink(ArchivedPageReqDTO requestParam);

    /**
     * 恢复归档的短链接
     * @param requestParam 恢复归档的短链接请求参数
     */
    void recoverShortLink(ArchiveRecoverDTO requestParam);
}
