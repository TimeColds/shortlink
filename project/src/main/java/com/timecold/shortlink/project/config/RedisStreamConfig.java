package com.timecold.shortlink.project.config;

import com.timecold.shortlink.project.mq.consumer.ShortLinkPlatfomrStatsConsumer;
import com.timecold.shortlink.project.mq.consumer.ShortLinkStatsConsumer;
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
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

import static com.timecold.shortlink.project.common.constant.RedisStreamConstant.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableScheduling
public class RedisStreamConfig implements InitializingBean, DisposableBean {

    private final RedisConnectionFactory redisConnectionFactory;
    private final ShortLinkStatsConsumer streamConsumer;
    private final ShortLinkPlatfomrStatsConsumer platformStreamConsumer;
    private final StringRedisTemplate stringRedisTemplate;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private Subscription statsSubscription;
    private Subscription platformStatsSubscription;
    private static final long MAX_STREAM_LENGTH = 10;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            stringRedisTemplate.opsForStream().createGroup(DAILY_STATS_STREAM, DAILY_CONSUMER_GROUP);
        } catch (Exception e) {
            log.info("每日统计流或消费者组已存在: {}", e.getMessage());
        }
        try {
            stringRedisTemplate.opsForStream().createGroup(PLATFORM_STATS_STREAM, PLATFORM_CONSUMER_GROUP);
        } catch (Exception e) {
            log.info("设备统计流或消费者组已存在: {}", e.getMessage());
        }
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .build();
        container = StreamMessageListenerContainer.create(redisConnectionFactory, options);
        statsSubscription = container.receive(
                Consumer.from(DAILY_CONSUMER_GROUP, "daily-consumer-1"),
                StreamOffset.create(DAILY_STATS_STREAM, ReadOffset.lastConsumed()),
                streamConsumer
        );
        platformStatsSubscription = container.receive(
                Consumer.from(PLATFORM_CONSUMER_GROUP, "device-consumer-1"),
                StreamOffset.create(PLATFORM_STATS_STREAM, ReadOffset.lastConsumed()),
                platformStreamConsumer
        );
        container.start();
        log.info("Redis流监听器容器已启动");
    }

    @Override
    public void destroy() throws Exception {
        if (statsSubscription != null) {
            statsSubscription.cancel();
        }
        if (platformStatsSubscription != null) {
            platformStatsSubscription.cancel();
        }
        if (container != null) {
            container.stop();
        }
        log.info("Redis流监听器容器已停止");
    }

    @Scheduled(fixedRate = 1,timeUnit = TimeUnit.HOURS) // Run every hour
    public void trimStream() {
        try {
            Long dailyTrim = stringRedisTemplate.opsForStream().trim(DAILY_STATS_STREAM, MAX_STREAM_LENGTH, true);
            log.info("修剪每日流 {} 移除了 {} 条记录",
                    DAILY_STATS_STREAM,dailyTrim);

            Long platformTrim = stringRedisTemplate.opsForStream().trim(PLATFORM_STATS_STREAM, MAX_STREAM_LENGTH, true);
            log.info("修剪设备流 {} ，移除了 {} 条记录",
                    PLATFORM_STATS_STREAM, platformTrim);
        } catch (Exception e) {
            log.error("修剪流时出错", e);
        }
    }
}
