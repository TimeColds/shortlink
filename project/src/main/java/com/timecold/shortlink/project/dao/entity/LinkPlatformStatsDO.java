package com.timecold.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.timecold.shortlink.project.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 短链接设备统计实体
 */
@TableName(value ="t_device_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkPlatformStatsDO extends BaseDO {
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
     * 统计日期
     */
    private LocalDate statsDate;

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
     * 访问量
     */
    private Long pv;
}