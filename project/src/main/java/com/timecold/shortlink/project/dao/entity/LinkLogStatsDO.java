package com.timecold.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.timecold.shortlink.project.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 短链接统计日志实体
 */
@TableName(value ="t_log_stats")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkLogStatsDO extends BaseDO {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 短链接
     */
    private String shortUrl;

    /**
     * 访问时间
     */
    private LocalDateTime accessTime;

    /**
     * 访问者IP地址
     */
    private String ip;

    /**
     * 来源页面
     */
    private String referer;

    /**
     * 设备类型
     */
    private String device;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 访客类型
     */
    private Integer visitorType;
}