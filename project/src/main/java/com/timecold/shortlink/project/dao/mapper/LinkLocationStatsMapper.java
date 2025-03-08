package com.timecold.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timecold.shortlink.project.dao.entity.LinkLocationStatsDO;
import com.timecold.shortlink.project.dto.biz.ShortLinkProvinceStatsDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

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


    @Select("""
        SELECT province, SUM(pv) AS count FROM t_location_stats
        WHERE short_url = #{shortUrl} AND stats_date BETWEEN #{beginDate} AND #{endDate} AND del_flag = 0
        GROUP BY province
        """)
    List<ShortLinkProvinceStatsDTO> getProvinceStats(@Param("shortUrl") String shortUrl,
                                                     @Param("beginDate") LocalDate beginDate,
                                                     @Param("endDate") LocalDate endDate);
}




