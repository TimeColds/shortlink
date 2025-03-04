package com.timecold.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timecold.shortlink.project.dao.entity.LinkPlatformStatsDO;
import org.apache.ibatis.annotations.Insert;

/**
 * 短链接设备统计持久层
 */
public interface LinkPlatformStatsMapper extends BaseMapper<LinkPlatformStatsDO> {

    @Insert("""
            INSERT INTO t_platform_stats (short_url, stats_date, device, os, browser, pv, create_time, update_time, del_flag)
                        VALUES (#{shortUrl}, #{statsDate},#{device}, #{os}, #{browser}, #{pv}, now(), now(), 0)
                        ON DUPLICATE KEY UPDATE
                        pv = #{pv},
                        update_time = now()
            """)
    void saveOrUpdate(LinkPlatformStatsDO linkPlatformStatsDO);
}




