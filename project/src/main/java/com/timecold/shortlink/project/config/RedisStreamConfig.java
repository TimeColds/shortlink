package com.timecold.shortlink.project.config;

import com.timecold.shortlink.project.mq.consumer.ShortLinkDailyStatsConsumer;
import com.timecold.shortlink.project.mq.consumer.ShortLinkLocationStatsConsumer;
import com.timecold.shortlink.project.mq.consumer.ShortLinkPlatformStatsConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.timecold.shortlink.project.common.constant.RedisStreamConstant.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableScheduling
public class RedisStreamConfig implements InitializingBean, DisposableBean {

    private final RedisConnectionFactory redisConnectionFactory;
    private final ShortLinkDailyStatsConsumer dailyStatsConsumer;
    private final ShortLinkPlatformStatsConsumer platformStatsConsumer;
    private final ShortLinkLocationStatsConsumer locationStatsConsumer;
    private final StringRedisTemplate stringRedisTemplate;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private static final String DAILY_CONSUMER_NAME = "daily-consumer-1";
    private static final String PLATFORM_CONSUMER_NAME = "platform-consumer-1";
    private static final String LOCATION_CONSUMER_NAME = "location-consumer-1";
    private static final long MAX_STREAM_LENGTH = 10;

    private static final Map<String, String> STREAM_GROUPS = Map.of(
            DAILY_STATS_STREAM, DAILY_CONSUMER_GROUP,
            PLATFORM_STATS_STREAM, PLATFORM_CONSUMER_GROUP,
            LOCATION_STATS_STREAM, LOCATION_CONSUMER_GROUP
    );

    @Override
    public void afterPropertiesSet() throws Exception {
        STREAM_GROUPS.forEach((stream, group) -> {
            try {
                stringRedisTemplate.opsForStream().createGroup(stream, group);
            } catch (Exception e) {
                log.info("流或消费者组已存在: {} - {}: {}", stream, group, e.getMessage());
            }
        });

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(100))
                        .build();
        container = StreamMessageListenerContainer.create(redisConnectionFactory, options);
        container.receive(
                Consumer.from(DAILY_CONSUMER_GROUP, DAILY_CONSUMER_NAME),
                StreamOffset.create(DAILY_STATS_STREAM, ReadOffset.lastConsumed()),
                dailyStatsConsumer
        );
        container.receive(
                Consumer.from(PLATFORM_CONSUMER_GROUP, PLATFORM_CONSUMER_NAME),
                StreamOffset.create(PLATFORM_STATS_STREAM, ReadOffset.lastConsumed()),
                platformStatsConsumer
        );
        container.receive(
                Consumer.from(LOCATION_CONSUMER_GROUP, LOCATION_CONSUMER_NAME),
                StreamOffset.create(LOCATION_STATS_STREAM, ReadOffset.lastConsumed()),
                locationStatsConsumer
        );
        container.start();
        log.info("Redis流监听器容器已启动");
    }

    @Override
    public void destroy() throws Exception {
        if (container != null) {
            container.stop();
        }
        log.info("Redis流监听器容器已停止");
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS) // Run every hour
    public void trimStream() {
        STREAM_GROUPS.keySet().forEach(stream -> {
            try {
                Long removedCount = stringRedisTemplate.opsForStream().trim(stream, MAX_STREAM_LENGTH, true);
                log.info("修剪后的流 {}: 移除了 {} 条记录", stream, removedCount);
            } catch (Exception e) {
                log.error("修剪{}流时出错: ", stream, e);
            }
        });
    }
}
