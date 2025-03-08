package com.timecold.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.timecold.shortlink.project.common.convention.exception.ServiceException;
import com.timecold.shortlink.project.dao.entity.LinkDailyStatsDO;
import com.timecold.shortlink.project.dao.mapper.*;
import com.timecold.shortlink.project.dto.biz.*;
import com.timecold.shortlink.project.dto.resp.ShortLinkChartStatsRespDTO;
import com.timecold.shortlink.project.dto.resp.ShortLinkDailyStatsRespDTO;
import com.timecold.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
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
    private final LinkLocationStatsMapper linkLocationStatsMapper;
    private final LinkPlatformStatsMapper linkPlatformStatsMapper;
    private final LinkVisitorStatsMapper linkVisitorStatsMapper;

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
        List<ShortLinkChartStatsRespDTO.ProvinceStats> provinceDistribution = provinceStats(shortUrl, beginDate, endDate, today);
        List<ShortLinkChartStatsRespDTO.BrowserStats> browserDistribution = browserStats(shortUrl, beginDate, endDate, today);
        List<ShortLinkChartStatsRespDTO.OsStats> osDistribution = osStats(shortUrl, beginDate, endDate, today);
        List<ShortLinkChartStatsRespDTO.DeviceStats> deviceDistribution = deviceStats(shortUrl, beginDate, endDate, today);
        Long newVisitorCount = newVisitorStats(shortUrl, beginDate, endDate, today);
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
        List<Long> hourStats = Arrays.stream(hourlyDistribution).boxed().collect(Collectors.toList());
        List<Long> weekdayStats = Arrays.stream(weekdayDistribution).boxed().collect(Collectors.toList());
        shortLinkChartStatsRespDTO.setHourStats(hourStats);
        shortLinkChartStatsRespDTO.setWeekdayStats(weekdayStats);
        shortLinkChartStatsRespDTO.setProvinceStats(provinceDistribution);
        shortLinkChartStatsRespDTO.setBrowserStats(browserDistribution);
        shortLinkChartStatsRespDTO.setOsStats(osDistribution);
        shortLinkChartStatsRespDTO.setDeviceStats(deviceDistribution);
        shortLinkChartStatsRespDTO.setNewVisitor(newVisitorCount);
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

    private List<ShortLinkChartStatsRespDTO.ProvinceStats> provinceStats(String shortUrl, LocalDate beginDate, LocalDate endDate, LocalDate today) {
        List<ShortLinkChartStatsRespDTO.ProvinceStats> provinceStatsList = new ArrayList<>();
        LocalDate newEndDate = endDate.isEqual(today) ? endDate.minusDays(1) : endDate;
        List<ShortLinkProvinceStatsDTO> provinceStatsDTOList = linkLocationStatsMapper.getProvinceStats(shortUrl, beginDate, newEndDate);
        HashMap<String, Long> provinceMap = new HashMap<>();
        for (ShortLinkProvinceStatsDTO provinceStat : provinceStatsDTOList) {
            provinceMap.put(provinceStat.getProvince(), provinceStat.getCount());
        }
        if (!today.isBefore(beginDate) && !today.isAfter(endDate)) {
            String provinceKey = LINK_LOCATION_KEY_PREFIX + shortUrl + ":" + today;
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(provinceKey);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String province = (String) entry.getKey();
                Long count = Long.parseLong((String) entry.getValue());
                provinceMap.merge(province, count, Long::sum);
            }
        }
        long sum = provinceMap.values().stream().mapToLong(Long::longValue).sum();
        for (Map.Entry<String, Long> stringLongEntry : provinceMap.entrySet()) {
            String key = stringLongEntry.getKey();
            Long value = stringLongEntry.getValue();
            ShortLinkChartStatsRespDTO.ProvinceStats provinceStats = new ShortLinkChartStatsRespDTO.ProvinceStats();
            provinceStats.setProvince(key);
            provinceStats.setCount(value);
            provinceStats.setRatio(Math.round((double) value / sum * 100.0) / 100.0);
            provinceStatsList.add(provinceStats);
        }
        return provinceStatsList;
    }

    private List<ShortLinkChartStatsRespDTO.BrowserStats> browserStats(String shortUrl, LocalDate beginDate, LocalDate endDate, LocalDate today) {
        List<ShortLinkChartStatsRespDTO.BrowserStats> browserStatsList = new ArrayList<>();
        LocalDate newEndDate = endDate.isEqual(today) ? endDate.minusDays(1) : endDate;
        List<ShortLinkBrowserStatsDTO> browserStatsDTOList = linkPlatformStatsMapper.getBrowserStats(shortUrl, beginDate, newEndDate);
        HashMap<String, Long> browserMap = new HashMap<>();
        for (ShortLinkBrowserStatsDTO browserStat : browserStatsDTOList) {
            browserMap.put(browserStat.getBrowser(), browserStat.getCount());
        }
        if (!today.isBefore(beginDate) && !today.isAfter(endDate)) {
            String browserKey = LINK_PLATFORM_KEY_PREFIX + shortUrl + ":" + today;
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(browserKey);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String platform = (String) entry.getKey();
                String[] split = platform.split(":");
                String browser = split[2];
                Long count = Long.parseLong((String) entry.getValue());
                browserMap.merge(browser, count, Long::sum);
            }
        }
        long sum = browserMap.values().stream().mapToLong(Long::longValue).sum();
        for (Map.Entry<String, Long> stringLongEntry : browserMap.entrySet()) {
            String key = stringLongEntry.getKey();
            Long value = stringLongEntry.getValue();
            ShortLinkChartStatsRespDTO.BrowserStats browserStats = new ShortLinkChartStatsRespDTO.BrowserStats();
            browserStats.setBrowser(key);
            browserStats.setCount(value);
            browserStats.setRatio(Math.round((double) value / sum * 100.0) / 100.0);
            browserStatsList.add(browserStats);
        }
        return browserStatsList;
    }

    private List<ShortLinkChartStatsRespDTO.OsStats> osStats(String shortUrl, LocalDate beginDate, LocalDate endDate, LocalDate today) {
        List<ShortLinkChartStatsRespDTO.OsStats> osStatsList = new ArrayList<>();
        LocalDate newEndDate = endDate.isEqual(today) ? endDate.minusDays(1) : endDate;
        List<ShortLinkOsStatsDTO> osStatsDTOList = linkPlatformStatsMapper.getOsStats(shortUrl, beginDate, newEndDate);
        HashMap<String, Long> osMap = new HashMap<>();
        for (ShortLinkOsStatsDTO osStat : osStatsDTOList) {
            osMap.put(osStat.getOs(), osStat.getCount());
        }
        if (!today.isBefore(beginDate) && !today.isAfter(endDate)) {
            String osKey = LINK_PLATFORM_KEY_PREFIX + shortUrl + ":" + today;
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(osKey);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String platform = (String) entry.getKey();
                String[] split = platform.split(":");
                String os = split[1];
                Long count = Long.parseLong((String) entry.getValue());
                osMap.merge(os, count, Long::sum);
            }
        }
        long sum = osMap.values().stream().mapToLong(Long::longValue).sum();
        for (Map.Entry<String, Long> stringLongEntry : osMap.entrySet()) {
            String key = stringLongEntry.getKey();
            Long value = stringLongEntry.getValue();
            ShortLinkChartStatsRespDTO.OsStats osStats = new ShortLinkChartStatsRespDTO.OsStats();
            osStats.setOs(key);
            osStats.setCount(value);
            osStats.setRatio(Math.round((double) value / sum * 100.0) / 100.0);
            osStatsList.add(osStats);
        }
        return osStatsList;
    }

    private List<ShortLinkChartStatsRespDTO.DeviceStats> deviceStats(String shortUrl, LocalDate beginDate, LocalDate endDate, LocalDate today) {
        List<ShortLinkChartStatsRespDTO.DeviceStats> deviceStatsList = new ArrayList<>();
        LocalDate newEndDate = endDate.isEqual(today) ? endDate.minusDays(1) : endDate;
        List<ShortLinkDeviceStatsDTO> deviceStatsDTOList = linkPlatformStatsMapper.getDeviceStats(shortUrl, beginDate, newEndDate);
        HashMap<String, Long> deviceMap = new HashMap<>();
        for (ShortLinkDeviceStatsDTO deviceStat : deviceStatsDTOList) {
            deviceMap.put(deviceStat.getDevice(), deviceStat.getCount());
        }
        if (!today.isBefore(beginDate) && !today.isAfter(endDate)) {
            String deviceKey = LINK_PLATFORM_KEY_PREFIX + shortUrl + ":" + today;
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(deviceKey);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String platform = (String) entry.getKey();
                String[] split = platform.split(":");
                String device = split[0];
                Long count = Long.parseLong((String) entry.getValue());
                deviceMap.merge(device, count, Long::sum);
            }
        }
        long sum = deviceMap.values().stream().mapToLong(Long::longValue).sum();
        for (Map.Entry<String, Long> stringLongEntry : deviceMap.entrySet()) {
            String key = stringLongEntry.getKey();
            Long value = stringLongEntry.getValue();
            ShortLinkChartStatsRespDTO.DeviceStats deviceStats = new ShortLinkChartStatsRespDTO.DeviceStats();
            deviceStats.setDevice(key);
            deviceStats.setCount(value);
            deviceStats.setRatio(Math.round((double) value / sum * 100.0) / 100.0);
            deviceStatsList.add(deviceStats);
        }
        return deviceStatsList;
    }

    private Long newVisitorStats(String shortUrl, LocalDate beginDate, LocalDate endDate, LocalDate today) {
        LocalDate newEndDate = endDate.isEqual(today) ? endDate.minusDays(1) : endDate;
        Long newVisitorCount = linkVisitorStatsMapper.getVisitorStats(shortUrl, beginDate, newEndDate);
        newVisitorCount = newVisitorCount == null ? 0L : newVisitorCount;
        if (!today.isBefore(beginDate) && !today.isAfter(endDate)) {
            String newVisitorKey = LINK_UV_KEY_PREFIX + shortUrl + ":new:" + today;
            String uvNewValue = stringRedisTemplate.opsForValue().get(newVisitorKey);
            long newUv = uvNewValue != null ? Long.parseLong(uvNewValue) : 0L;
            newVisitorCount += newUv;
        }
        return newVisitorCount;
    }

}
