package com.timecold.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timecold.shortlink.project.dao.entity.LinkDailyStatsDO;
import org.apache.ibatis.annotations.Insert;

/**
 * 短链接每日统计持久层
 */
public interface LinkDailyStatsMapper extends BaseMapper<LinkDailyStatsDO> {

    @Insert("""
            INSERT INTO t_daily_stats (short_url, stats_date, hour, pv, uv, uip, create_time, update_time,del_flag)
                        VALUES (#{shortUrl}, #{statsDate}, #{hour}, #{pv}, #{uv}, #{uip}, now(), now(), 0)
                        ON DUPLICATE KEY UPDATE
                        pv = #{pv},
                        uv = #{uv},
                        uip = #{uip},
                        update_time = now()
           """)
    void saveOrUpdate(LinkDailyStatsDO linkDailyStatsDO);

}




