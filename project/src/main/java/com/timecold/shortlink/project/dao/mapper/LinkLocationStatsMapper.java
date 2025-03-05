package com.timecold.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timecold.shortlink.project.dao.entity.LinkLocationStatsDO;
import org.apache.ibatis.annotations.Insert;

/**
 * 短链接地区统计持久层
 */
public interface LinkLocationStatsMapper extends BaseMapper<LinkLocationStatsDO> {

    @Insert("""
            INSERT INTO t_location_stats (short_url, stats_date,province, pv,create_time, update_time,del_flag)
                        VALUES (#{shortUrl}, #{statsDate},#{province}, #{pv}, now(), now(), 0)
                        ON DUPLICATE KEY UPDATE
                        pv = #{pv},
                        update_time = now()
           """)
    void saveOrUpdate(LinkLocationStatsDO linkLocationStatsDO);
}




