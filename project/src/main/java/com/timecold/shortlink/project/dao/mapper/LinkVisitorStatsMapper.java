package com.timecold.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timecold.shortlink.project.dao.entity.LinkVisitorStatsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

public interface LinkVisitorStatsMapper extends BaseMapper<LinkVisitorStatsDO> {

    @Insert("""
            INSERT INTO t_visitor_stats (short_url, stats_date, new_uv, create_time, update_time, del_flag)
                        VALUES (#{shortUrl}, #{statsDate},#{newUv}, now(), now(), 0)
                        ON DUPLICATE KEY UPDATE
                        new_uv = #{newUv},
                        update_time = now()
            """)
    void saveOrUpdate(LinkVisitorStatsDO linkVisitorStatsDO);


    @Select("""
            select  SUM(new_uv) as sum from t_visitor_stats
            WHERE short_url = #{shortUrl} AND stats_date BETWEEN #{beginDate} AND #{endDate} AND del_flag = 0
            group by short_url
            """)
    Long getVisitorStats(@Param("shortUrl") String shortUrl,
                                                   @Param("beginDate") LocalDate beginDate,
                                                   @Param("endDate") LocalDate endDate);
}




