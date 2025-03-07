package com.timecold.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.timecold.shortlink.project.common.convention.exception.ServiceException;
import com.timecold.shortlink.project.dao.entity.LinkDailyStatsDO;
import com.timecold.shortlink.project.dao.mapper.LinkDailyStatsMapper;
import com.timecold.shortlink.project.dao.mapper.LinkLogStatsMapper;
import com.timecold.shortlink.project.dto.biz.ShortLinkHourlyStatsDTO;
import com.timecold.shortlink.project.dto.biz.ShortLinkWeeklyStatsDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkChartStatsRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkDailyStatsRespDTO;
import com.timecold.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.timecold.shortlink.project.common.constant.RedisKeyConstant.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl implements ShortLinkStatsService {

    private final RBloomFilter<String> shortUrlCachePenetrationBloomFilter;
    private final StringRedisTemplate stringRedisTemplate;
    private final LinkLogStatsMapper linkLogStatsMapper;
    private final LinkDailyStatsMapper linkDailyStatsMapper;

    @Override
    public ShortLinkDailyStatsRespDTO getDailyStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {

        if (!shortUrlCachePenetrationBloomFilter.contains(shortUrl)) {
            throw new ServiceException("短链接不存在");
        }
        List<LinkDailyStatsDO> linkDailyStatsDOList = linkDailyStatsMapper.selectList(Wrappers.query(LinkDailyStatsDO.class)
                .select("stats_date",
                        "SUM(pv) AS pv",
                        "SUM(uv) AS uv",
                        "SUM(uip) AS uip")
                .eq("short_url", shortUrl)
                .between("stats_date", beginDate, endDate)
                .eq("del_flag", 0)
                .groupBy("stats_date"));
        Map<LocalDate, LinkDailyStatsDO> statsByDate = linkDailyStatsDOList.stream()
                .collect(Collectors.toMap(LinkDailyStatsDO::getStatsDate, Function.identity()));

        LocalDate now = LocalDate.now();
        long todayPv = 0L, todayUv = 0L, todayUip = 0L;

        if (!now.isBefore(beginDate) && !now.isAfter(endDate)) {
            String pvKey = LINK_PV_KEY_PREFIX + shortUrl + ":" + now;
            String uvKey = LINK_UV_KEY_PREFIX + shortUrl + ":" + now;
            String uipKey = LINK_UIP_KEY_PREFIX + shortUrl + ":" + now;
            Object pvResult = stringRedisTemplate.opsForHash().get(pvKey, "total");
            if (pvResult != null) {
                todayPv = Long.parseLong(pvResult.toString());
            }
            todayUv = stringRedisTemplate.opsForHyperLogLog().size(uvKey);
            todayUip = stringRedisTemplate.opsForHyperLogLog().size(uipKey);
        }

        long totalPv = 0L, totalUv = 0L, totalUip = 0L;
        LocalDate currentDate = beginDate;
        List<ShortLinkDailyStatsRespDTO.DailyStats> dailyStats = new ArrayList<>();
        while (!currentDate.isAfter(endDate)) {
            long pv, uv, uip;
            if (currentDate.equals(now)) {
                pv = todayPv;
                uv = todayUv;
                uip = todayUip;
            } else {
                LinkDailyStatsDO dayStats = statsByDate.get(currentDate);
                pv = dayStats == null ? 0L : dayStats.getPv();
                uv = dayStats == null ? 0L : dayStats.getUv();
                uip = dayStats == null ? 0L : dayStats.getUip();
            }
            totalPv += pv;
            totalUv += uv;
            totalUip += uip;

            ShortLinkDailyStatsRespDTO.DailyStats currentDayData = new ShortLinkDailyStatsRespDTO.DailyStats();
            currentDayData.setDate(currentDate);
            currentDayData.setPv(pv);
            currentDayData.setUv(uv);
            currentDayData.setUip(uip);
            dailyStats.add(currentDayData);

            currentDate = currentDate.plusDays(1);
        }
        ShortLinkDailyStatsRespDTO dailyStatsRespDTO = new ShortLinkDailyStatsRespDTO();
        ShortLinkDailyStatsRespDTO.StatsAll statsAll = new ShortLinkDailyStatsRespDTO.StatsAll();
        statsAll.setPv(totalPv);
        statsAll.setUv(totalUv);
        statsAll.setUip(totalUip);
        dailyStatsRespDTO.setStatsAll(statsAll);
        dailyStatsRespDTO.setDailyStats(dailyStats);
        return dailyStatsRespDTO;
    }

    @Override
    public ShortLinkChartStatsRespDTO getChartStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        if (!shortUrlCachePenetrationBloomFilter.contains(shortUrl)) {
            throw new ServiceException("短链接不存在");
        }
        long[] hourlyDistribution = new long[24];
        long[] weekdayDistribution = new long[7];
        LocalDate today = LocalDate.now();
        if (beginDate.isBefore(today) || endDate.isBefore(today)) {
            LocalDate newEndDate = endDate.isEqual(today) ? endDate.minusDays(1) : endDate;
            hourlyDistribution = hourlyStats(shortUrl, beginDate, newEndDate);
            weekdayDistribution = weeklyStats(shortUrl, beginDate, newEndDate);
        }
        if (!today.isBefore(beginDate) && !today.isAfter(endDate)) {
            String pvKey = LINK_PV_KEY_PREFIX + shortUrl + ":" + today;
            Object result = stringRedisTemplate.opsForHash().get(pvKey, "total");
            int dayIndex = today.getDayOfWeek().getValue() - 1;
            if (result != null) {
                weekdayDistribution[dayIndex] += Long.parseLong(result.toString());
            }
            Map<Object, Object> hourlyPv = stringRedisTemplate.opsForHash().entries(pvKey);
            hourlyPv.remove("total");
            for (Map.Entry<Object, Object> entry : hourlyPv.entrySet()) {
                int hour = Integer.parseInt(entry.getKey().toString());
                long pv = Long.parseLong(entry.getValue().toString());
                hourlyDistribution[hour] += pv;
            }
        }
        ShortLinkChartStatsRespDTO shortLinkChartStatsRespDTO = new ShortLinkChartStatsRespDTO();
        List<Long> hourStats = Arrays.stream(hourlyDistribution)
                .boxed()
                .collect(Collectors.toList());
        List<Long> weekdayStats = Arrays.stream(weekdayDistribution).boxed().collect(Collectors.toList());
        shortLinkChartStatsRespDTO.setHourStats(hourStats);
        shortLinkChartStatsRespDTO.setWeekdayStats(weekdayStats);
        return shortLinkChartStatsRespDTO;
    }




    private long[] weeklyStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        List<ShortLinkWeeklyStatsDTO> weeklyStatsDTOList = linkDailyStatsMapper.getWeeklyStats(shortUrl, beginDate, endDate);
        long[] weekdayDistribution = new long[7];
        for (ShortLinkWeeklyStatsDTO weeklyStats : weeklyStatsDTOList) {
            int weekIndex = weeklyStats.getDayOfWeek();
            weekdayDistribution[weekIndex] += weeklyStats.getPv();
        }
        return weekdayDistribution;
    }
    private long[] hourlyStats(String shortUrl, LocalDate beginDate, LocalDate endDate) {
        List<ShortLinkHourlyStatsDTO> hourlyStatsDTOList = linkDailyStatsMapper.getHourlyStats(shortUrl, beginDate, endDate);
        long[] hourlyDistribution = new long[24];
        for (ShortLinkHourlyStatsDTO hourlyStats : hourlyStatsDTOList) {
            int hourIndex = hourlyStats.getHour();
            hourlyDistribution[hourIndex] += hourlyStats.getPv();
        }
        return hourlyDistribution;
    }

}
