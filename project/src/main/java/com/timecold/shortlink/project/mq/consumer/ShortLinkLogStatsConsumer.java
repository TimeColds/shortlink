package com.timecold.shortlink.project.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecold.shortlink.project.dao.entity.LinkLogStatsDO;
import com.timecold.shortlink.project.dao.mapper.LinkLogStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.timecold.shortlink.project.common.constant.RedisStreamConstant.LOG_STATS_CONSUMER_GROUP;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkLogStatsConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate stringRedisTemplate;
    private final LinkLogStatsMapper linkLogStatsMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Map<String, String> value = message.getValue();
            String linkStatsLog = value.get("linkStatsLog");
            LinkLogStatsDO linkLogStatsDO = objectMapper.readValue(linkStatsLog, LinkLogStatsDO.class);
            linkLogStatsMapper.insert(linkLogStatsDO);
            stringRedisTemplate.opsForStream().acknowledge(LOG_STATS_CONSUMER_GROUP, message);
        } catch (Exception e) {
            log.error("处理统计日志流消息时出错", e);
        }
    }
}
