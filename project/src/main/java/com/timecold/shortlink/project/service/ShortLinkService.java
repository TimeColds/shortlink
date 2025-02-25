package com.timecold.shortlink.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timecold.shortlink.project.dao.entity.ShortLinkDO;
import com.timecold.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.timecold.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.timecold.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;


/**
 * 短链接接口层
 */
public interface ShortLinkService extends IService<ShortLinkDO> {

    /**
     * 创建短链接
     * @param requestParam 创建短链接请求参数
     * @return 短链接创建信息
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);


    /**
     * 分页查询短链接
     * @param requestParam 分页查询短链接请求参数
     * @return 短链接分页返回结果
     */
    Page<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);

    /**
     * 查询用户每个分组内短链接数量
     *
     * @param uid 用户id
     * @return 每个分组内短链接数量响应
     */
    List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(Long uid);


    /**
     * 修改短链接
     * @param requestParam 修改短链接请求参数
     */
    void updateShortLink(ShortLinkUpdateReqDTO requestParam);

    /**
     * 短链接跳转
     * @param shortUrl 短链接后缀
     * @param response HTTP 响应
     */
    void redirectUrl(String shortUrl, HttpServletResponse response);

    /**
     * 根据 URL 获取标题
     *
     * @param url 目标网站地址
     * @return 网站标题
     */
    String getTitleByUrl(String url);
}
