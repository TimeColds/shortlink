package com.timecold.shortlink.project.mq.consumer;

import com.timecold.shortlink.project.dao.entity.LinkLocationStatsDO;
import com.timecold.shortlink.project.dao.mapper.LinkLocationStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

import static com.timecold.shortlink.project.common.constant.RedisKeyConstant.LINK_LOCATION_KEY_PREFIX;
import static com.timecold.shortlink.project.common.constant.RedisStreamConstant.LOCATION_CONSUMER_GROUP;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkLocationStatsConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate stringRedisTemplate;
    private final LinkLocationStatsMapper linkLocationStatsMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Map<String, String> value = message.getValue();
            String shortUrl = value.get("shortUrl");
            String dateStr = value.get("date");
            LocalDate date = LocalDate.parse(dateStr);

            String locationStatsKey = LINK_LOCATION_KEY_PREFIX + shortUrl + ":" + dateStr;
            Map<Object, Object> locationStats = stringRedisTemplate.opsForHash().entries(locationStatsKey);

            for (Map.Entry<Object, Object> entry : locationStats.entrySet()) {
                String locationKey = entry.getKey().toString();
                Long count = Long.parseLong(entry.getValue().toString());
                LinkLocationStatsDO stats = LinkLocationStatsDO.builder()
                        .shortUrl(shortUrl)
                        .statsDate(date)
                        .province(locationKey)
                        .pv(count)
                        .build();

                linkLocationStatsMapper.saveOrUpdate(stats);
            }
            stringRedisTemplate.opsForStream().acknowledge(LOCATION_CONSUMER_GROUP, message);
        } catch (Exception e) {
            log.error("处理设备统计流消息时出错", e);
        }
    }
}
