package com.timecold.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.timecold.shortlink.project.common.convention.exception.ServiceException;
import com.timecold.shortlink.project.dao.entity.LinkDailyStatsDO;
import com.timecold.shortlink.project.dao.mapper.LinkDailyStatsMapper;
import com.timecold.shortlink.project.dao.mapper.LinkLogStatsMapper;
import com.timecold.shortlink.project.dto.resp.ShortLinkChartStatsRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkDailyStatsRespDTO;
import com.timecold.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
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
        LambdaQueryWrapper<LinkDailyStatsDO> lambdaQueryWrapper = Wrappers.lambdaQuery(LinkDailyStatsDO.class)
                .eq(LinkDailyStatsDO::getShortUrl, shortUrl)
                .between(LinkDailyStatsDO::getStatsDate, beginDate, endDate)
                .eq(LinkDailyStatsDO::getDelFlag, 0);
        List<LinkDailyStatsDO> linkDailyStatsDOList = linkDailyStatsMapper.selectList(lambdaQueryWrapper);
        Map<LocalDate, List<LinkDailyStatsDO>> statsByDate = linkDailyStatsDOList.stream()
                .collect(Collectors.groupingBy(LinkDailyStatsDO::getStatsDate));
        long totalPv = 0;
        long totalUv = 0;
        long totalUip = 0;
        LocalDate now = LocalDate.now();
        List<ShortLinkDailyStatsRespDTO.DailyStats> dailyStats = new ArrayList<>();

        long todayPv = 0L;
        long todayUv = 0L;
        long todayUip = 0L;
        if (!now.isBefore(beginDate) && !now.isAfter(endDate)) {
            String pvKey = LINK_PV_KEY_PREFIX + shortUrl + ":" + now;
            String uvKey = LINK_UV_KEY_PREFIX + shortUrl + ":" + now;
            String uipKey = LINK_UIP_KEY_PREFIX + shortUrl + ":" + now;
            Object result = stringRedisTemplate.opsForHash().get(pvKey, "total");
            if (result != null) {
                todayPv = Long.parseLong(result.toString());
            }
            todayUv = stringRedisTemplate.opsForHyperLogLog().size(uvKey);
            todayUip = stringRedisTemplate.opsForHyperLogLog().size(uipKey);
        }

        LocalDate currentDate = beginDate;
        while (!currentDate.isAfter(endDate)) {
            ShortLinkDailyStatsRespDTO.DailyStats currentDayData = new ShortLinkDailyStatsRespDTO.DailyStats();
            currentDayData.setDate(currentDate);

            long pv, uv, uip;
            if (currentDate.equals(now)) {
                pv = todayPv;
                uv = todayUv;
                uip = todayUip;
            } else {
                List<LinkDailyStatsDO> dayStats = statsByDate.getOrDefault(currentDate, Collections.emptyList());
                pv = dayStats.stream().mapToLong(LinkDailyStatsDO::getPv).sum();
                uv = dayStats.stream().mapToLong(LinkDailyStatsDO::getUv).sum();
                uip = dayStats.stream().mapToLong(LinkDailyStatsDO::getUip).sum();
            }

            totalPv += pv;
            totalUv += uv;
            totalUip += uip;
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
            LambdaQueryWrapper<LinkDailyStatsDO> lambdaQueryWrapper = Wrappers.lambdaQuery(LinkDailyStatsDO.class)
                    .eq(LinkDailyStatsDO::getShortUrl, shortUrl)
                    .between(LinkDailyStatsDO::getStatsDate, beginDate, newEndDate)
                    .eq(LinkDailyStatsDO::getDelFlag, 0)
                    .orderByAsc(LinkDailyStatsDO::getHour);
            List<LinkDailyStatsDO> linkDailyStatsDOList = linkDailyStatsMapper.selectList(lambdaQueryWrapper);
            for (LinkDailyStatsDO stats : linkDailyStatsDOList) {
                hourlyDistribution[stats.getHour()] += stats.getPv();
            }
            weekdayDistribution = weekdayStats(linkDailyStatsDOList, beginDate, newEndDate);
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


    private long[] weekdayStats(List<LinkDailyStatsDO> linkDailyStatsDOList,LocalDate beginDate, LocalDate endDate) {
        Map<LocalDate, List<LinkDailyStatsDO>> statsByDate = linkDailyStatsDOList.stream()
                .collect(Collectors.groupingBy(LinkDailyStatsDO::getStatsDate));
        long[] weekdayDistribution = new long[7];
        LocalDate currentDate = beginDate;
        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            int dayIndex = dayOfWeek.getValue() - 1;
                List<LinkDailyStatsDO> dayStats = statsByDate.getOrDefault(currentDate, Collections.emptyList());
                long pv = dayStats.stream().mapToLong(LinkDailyStatsDO::getPv).sum();
                weekdayDistribution[dayIndex] += pv;
            currentDate = currentDate.plusDays(1);
        }
        return weekdayDistribution;
    }
}
