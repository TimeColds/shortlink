package com.timecold.shortlink.project.mq.consumer;

import com.timecold.shortlink.project.dao.entity.LinkPlatformStatsDO;
import com.timecold.shortlink.project.dao.mapper.LinkPlatformStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

import static com.timecold.shortlink.project.common.constant.RedisKeyConstant.LINK_PLATFORM_KEY_PREFIX;
import static com.timecold.shortlink.project.common.constant.RedisStreamConstant.PLATFORM_CONSUMER_GROUP;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkPlatfomrStatsConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate stringRedisTemplate;
    private final LinkPlatformStatsMapper linkPlatformStatsMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Map<String, String> value = message.getValue();
            String shortUrl = value.get("shortUrl");
            String dateStr = value.get("date");

            LocalDate date = LocalDate.parse(dateStr);

            String platformStatsKey = LINK_PLATFORM_KEY_PREFIX + shortUrl + ":" + dateStr;
            Map<Object, Object> platformStats = stringRedisTemplate.opsForHash().entries(platformStatsKey);

            for (Map.Entry<Object, Object> entry : platformStats.entrySet()) {
                String platformKey = entry.getKey().toString();
                Long count = Long.parseLong(entry.getValue().toString());

                String[] parts = platformKey.split(":");
                if (parts.length != 3) {
                    log.error("无效的平台键格式: {}", platformKey);
                    continue;
                }

                String device = parts[0];
                String os = parts[1];
                String browser = parts[2];

                LinkPlatformStatsDO stats = LinkPlatformStatsDO.builder()
                        .shortUrl(shortUrl)
                        .statsDate(date)
                        .device(device)
                        .os(os)
                        .browser(browser)
                        .pv(count)
                        .build();

                linkPlatformStatsMapper.saveOrUpdate(stats);
            }
            stringRedisTemplate.opsForStream().acknowledge(PLATFORM_CONSUMER_GROUP, message);
        } catch (Exception e) {
            log.error("处理设备统计流消息时出错", e);
        }
    }
}
