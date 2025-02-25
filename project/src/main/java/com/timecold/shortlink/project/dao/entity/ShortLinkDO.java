package com.timecold.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.timecold.shortlink.project.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 短链接实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_link")
public class ShortLinkDO extends BaseDO {
    /**
     * ID
     */
    private Long id;

    /**
     * 域名
     */
    private String domain;

    /**
     * 短链接
     */
    private String shortUrl;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 点击量
     */
    private Integer clickNumber;

    /**
     * 用户id
     */
    private Long uid;

    /**
     * 分组标识
     */
    private Long gid;

    /**
     * 网站图标
     */
    private String favicon;

    /**
     * 启用标识 0:启用 1:未启用
     */
    private Integer enableStatus;

    /**
     * 创建类型 0:接口创建 1:控制台创建
     */
    private Integer createdType;

    /**
     * 有效期
     */
    private Date validDate;

    /**
     * 描述
     */
    @TableField("`describe`")
    private String describe;
}
