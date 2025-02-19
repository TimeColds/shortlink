package com.timecold.shortlink.admin.remote.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.remote.dto.req.RecycleBinArchiveReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.timecold.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
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
    Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParam);

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
    Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam);

    /**
     * 查询分组短链接总量
     *
     * @param requestParam 分组短链接总量请求参数
     * @return 查询分组短链接总量响应
     */
    Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(List<String> requestParam);

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
    void archiveShortLink(RecycleBinArchiveReqDTO requestParam);
}
