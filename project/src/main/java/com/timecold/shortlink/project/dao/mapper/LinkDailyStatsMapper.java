package com.timecold.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timecold.shortlink.project.dao.entity.LinkDailyStatsDO;
import com.timecold.shortlink.project.dto.biz.ShortLinkHourlyStatsDTO;
import com.timecold.shortlink.project.dto.biz.ShortLinkWeeklyStatsDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

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


    @Select("""
            SELECT WEEKDAY(stats_date) AS dayOfWeek, SUM(pv) AS pv FROM t_daily_stats
            WHERE short_url = #{shortUrl} AND stats_date BETWEEN #{beginDate} AND #{endDate} AND del_flag = 0
            GROUP BY stats_date
            """)
    List<ShortLinkWeeklyStatsDTO> getWeeklyStats(@Param("shortUrl") String shortUrl,
                                                 @Param("beginDate") LocalDate beginDate,
                                                 @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT hour, SUM(pv) AS pv FROM t_daily_stats
            WHERE short_url = #{shortUrl} AND stats_date BETWEEN #{beginDate} AND #{endDate} AND del_flag = 0
            GROUP BY hour
            """)
    List<ShortLinkHourlyStatsDTO> getHourlyStats(@Param("shortUrl") String shortUrl,
                                                 @Param("beginDate") LocalDate beginDate,
                                                 @Param("endDate") LocalDate endDate);

}




