package com.timecold.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timecold.shortlink.project.dao.entity.LinkPlatformStatsDO;
import com.timecold.shortlink.project.dto.biz.ShortLinkBrowserStatsDTO;
import com.timecold.shortlink.project.dto.biz.ShortLinkDeviceStatsDTO;
import com.timecold.shortlink.project.dto.biz.ShortLinkOsStatsDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

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


    @Select("""
            select browser, SUM(pv) as count from t_platform_stats
            WHERE short_url = #{shortUrl} AND stats_date BETWEEN #{beginDate} AND #{endDate} AND del_flag = 0
            GROUP BY browser
            """)
    List<ShortLinkBrowserStatsDTO> getBrowserStats(@Param("shortUrl") String shortUrl,
                                                   @Param("beginDate") LocalDate beginDate,
                                                   @Param("endDate") LocalDate endDate);
    @Select("""
            select os, SUM(pv) as count from t_platform_stats
            WHERE short_url = #{shortUrl} AND stats_date BETWEEN #{beginDate} AND #{endDate} AND del_flag = 0
            GROUP BY os
            """)
    List<ShortLinkOsStatsDTO> getOsStats(@Param("shortUrl") String shortUrl,
                                  @Param("beginDate") LocalDate beginDate,
                                  @Param("endDate") LocalDate endDate);
    @Select("""
            select device, SUM(pv) as count from t_platform_stats
            WHERE short_url = #{shortUrl} AND stats_date BETWEEN #{beginDate} AND #{endDate} AND del_flag = 0
            GROUP BY device
            """)
    List<ShortLinkDeviceStatsDTO> getDeviceStats(@Param("shortUrl") String shortUrl,
                                                 @Param("beginDate") LocalDate beginDate,
                                                 @Param("endDate") LocalDate endDate);
}




