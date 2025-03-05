package com.timecold.shortlink.project.mq.consumer;

import com.timecold.shortlink.project.dao.entity.LinkDailyStatsDO;
import com.timecold.shortlink.project.dao.mapper.LinkDailyStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

import static com.timecold.shortlink.project.common.constant.RedisKeyConstant.*;
import static com.timecold.shortlink.project.common.constant.RedisStreamConstant.DAILY_CONSUMER_GROUP;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkDailyStatsConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate stringRedisTemplate;
    private final LinkDailyStatsMapper linkDailyStatsMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Map<String, String> value = message.getValue();
            String shortUrl = value.get("shortUrl");
            String dateStr = value.get("date");
            Integer hour = Integer.valueOf(value.get("hour"));
            LocalDate date = LocalDate.parse(dateStr);

            String pvKey = LINK_PV_KEY_PREFIX + shortUrl + ":" + dateStr;
            String uvKey = LINK_UV_KEY_PREFIX + shortUrl + ":" + dateStr;
            String uipKey = LINK_UIP_KEY_PREFIX + shortUrl + ":" + dateStr;

            Object pvValue = stringRedisTemplate.opsForHash().get(pvKey, hour.toString());
            Long pv = pvValue != null ? Long.parseLong(pvValue.toString()) : 0L;
            Long uv = stringRedisTemplate.opsForHyperLogLog().size(uvKey);
            Long uip = stringRedisTemplate.opsForHyperLogLog().size(uipKey);
            LinkDailyStatsDO stats = LinkDailyStatsDO.builder()
                    .shortUrl(shortUrl)
                    .statsDate(date)
                    .hour(hour)
                    .pv(pv)
                    .uv(uv)
                    .uip(uip)
                    .build();
            linkDailyStatsMapper.saveOrUpdate(stats);
            stringRedisTemplate.opsForStream().acknowledge(DAILY_CONSUMER_GROUP, message);
        } catch (Exception e) {
            log.error("处理每日流消息时出错", e);
        }
    }
}
