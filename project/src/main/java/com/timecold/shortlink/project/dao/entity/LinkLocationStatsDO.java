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
 * 短链接地区统计实体
 */
@TableName(value ="t_location_stats")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkLocationStatsDO extends BaseDO {
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
     * 省份
     */
    private String province;

    /**
     * 访问量
     */
    private Long pv;
}