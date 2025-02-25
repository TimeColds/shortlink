package com.timecold.shortlink.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.timecold.shortlink.admin.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接分组实体
 */
@Data
@TableName("t_group")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupDO extends BaseDO {
    /**
     * id
     */
    private Long id;

    /**
     * 分组标识
     */
    private Long gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 创建分组用户id
     */
    private Long uid;

    /**
     * 分组排序
     */
    private Integer sortOrder;
}
