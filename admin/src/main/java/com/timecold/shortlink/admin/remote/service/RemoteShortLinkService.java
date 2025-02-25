package com.timecold.shortlink.admin.remote.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timecold.shortlink.admin.remote.dto.req.*;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.timecold.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

import java.util.List;

public interface RemoteShortLinkService {


    /**
     * 创建短链接
     *
     * @param requestParam 创建短链接请求参数
     * @return 短链接创建响应
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

    /**
     * 修改短链接
     *
     * @param requestParam 修改短链接请求参数
     */
    void updateShortLink(ShortLinkUpdateReqDTO requestParam);

    /**
     * 分页查询短链接
     *
     * @param requestParam 分页短链接请求参数
     * @return 查询短链接响应
     */
    Page<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);

    /**
     * 查询用户每个分组内短链接数量
     *
     * @return 每个分组内短链接数量响应
     */
    List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount();

    /**
     * 根据 URL 获取标题
     *
     * @param url 目标网站地址
     * @return 网站标题
     */
    String getTitleByUrl(String url);

    /**
     *    归档短链接
     *
     * @param requestParam 归档短链接请求参数
     */
    void archiveShortLink(ArchiveReqDTO requestParam);

    /**
     * 分页查询归档的短链接
     * @param size 每页显示条数
     * @param current 当前页
     * @return
     */
    Page<ShortLinkPageRespDTO> pageArchivedShortLink(Long size, Long current);

    /**
     * 恢复归档的短链接
     * @param requestParam 恢复归档的短链接请求参数
     */
    void recoverShortLink(ArchiveRecoverReqDTO requestParam);

    /**
     * 删除归档的短链接
     * @param requestParam 删除归档的短链接请求参数
     */
    void archiveRemove(ArchiveRemoveReqDTO requestParam);
}
