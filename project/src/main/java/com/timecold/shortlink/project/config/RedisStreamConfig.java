package com.timecold.shortlink.project.config;

import com.timecold.shortlink.project.mq.consumer.ShortLinkDailyStatsConsumer;
import com.timecold.shortlink.project.mq.consumer.ShortLinkLocationStatsConsumer;
import com.timecold.shortlink.project.mq.consumer.ShortLinkLogStatsConsumer;
import com.timecold.shortlink.project.mq.consumer.ShortLinkPlatformStatsConsumer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.timecold.shortlink.project.common.constant.RedisStreamConstant.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableScheduling
public class RedisStreamConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final ShortLinkDailyStatsConsumer dailyStatsConsumer;
    private final ShortLinkPlatformStatsConsumer platformStatsConsumer;
    private final ShortLinkLocationStatsConsumer locationStatsConsumer;
    private final ShortLinkLogStatsConsumer logStatsConsumer;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DAILY_CONSUMER_NAME = "daily-consumer-1";
    private static final String PLATFORM_CONSUMER_NAME = "platform-consumer-1";
    private static final String LOCATION_CONSUMER_NAME = "location-consumer-1";
    private static final String LOG_CONSUMER_NAME = "log-consumer-1";
    private static final long MAX_STREAM_LENGTH = 10;

    private static final Map<String, String> STREAM_GROUPS = Map.of(
            DAILY_STATS_STREAM, DAILY_CONSUMER_GROUP,
            PLATFORM_STATS_STREAM, PLATFORM_CONSUMER_GROUP,
            LOCATION_STATS_STREAM, LOCATION_CONSUMER_GROUP,
            LOG_STATS_STREAM, LOG_STATS_CONSUMER_GROUP
    );

    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer() {
        AtomicInteger index = new AtomicInteger(1);
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = getThreadPoolExecutor(corePoolSize, index);
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        // 一次最多获取多少条消息
                        .batchSize(10)
                        // 运行 Stream 的 poll task
                        .executor(executor)
                        // Stream 中没有消息时，阻塞多长时间，需要比 `spring.redis.timeout` 的时间小
                        .pollTimeout(Duration.ofSeconds(1))
                        // 获取消息的过程或获取到消息给具体的消息者处理的过程中，发生了异常的处理
                        .errorHandler(e -> log.error("处理流消息时出错", e))
                        .build();
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        registerConsumer(container, DAILY_STATS_STREAM, DAILY_CONSUMER_GROUP, DAILY_CONSUMER_NAME, dailyStatsConsumer);
        registerConsumer(container, LOCATION_STATS_STREAM, LOCATION_CONSUMER_GROUP, LOCATION_CONSUMER_NAME, locationStatsConsumer);
        registerConsumer(container, LOG_STATS_STREAM, LOG_STATS_CONSUMER_GROUP, LOG_CONSUMER_NAME, logStatsConsumer);
        registerConsumer(container, PLATFORM_STATS_STREAM, PLATFORM_CONSUMER_GROUP, PLATFORM_CONSUMER_NAME, platformStatsConsumer);
        return container;
    }
    /**
     * 注册Redis Stream消费者
     *
     * @param container 流消息监听容器
     * @param stream 流名称
     * @param consumerGroup 消费者组名称
     * @param consumerName 消费者名称
     * @param streamListener 流消息监听器
     */
    private void registerConsumer(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            String stream,
            String consumerGroup,
            String consumerName,
            StreamListener<String, MapRecord<String, String, String>> streamListener) {

        StreamMessageListenerContainer.StreamReadRequest<String> streamReadRequest =
                StreamMessageListenerContainer.StreamReadRequest
                        .builder(StreamOffset.create(stream, ReadOffset.lastConsumed()))
                        .consumer(Consumer.from(consumerGroup, consumerName))
                        .autoAcknowledge(false)
                        .cancelOnError(throwable -> false)
                        .build();

        container.register(streamReadRequest, streamListener);
        log.info("已注册Stream消费者: {}，消费者组: {}，流: {}", consumerName, consumerGroup, stream);
    }

    private static ThreadPoolExecutor getThreadPoolExecutor(int corePoolSize, AtomicInteger index) {
        int maxPoolSize = corePoolSize * 2;
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(2000),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("redis-stream-consumer-" + index.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @PostConstruct
    public void initConsumerGroups() {

        STREAM_GROUPS.forEach((stream, group) -> {
            try {
                stringRedisTemplate.opsForStream().createGroup(stream, group);
                log.info("创建消费者组 [{}] 成功，流名称: {}", group, stream);
            } catch (Exception e) {
                if (e.getCause().getMessage().contains("BUSYGROUP")) {
                    log.info("消费者组 [{}] 已存在，流名称: {}", group, stream);
                } else {
                    log.error("创建消费者组 [{}] 失败，流名称: {}", group, stream, e);
                }
            }
        });
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
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

/*    @Override
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
                        .batchSize(10)
                        .errorHandler(e -> log.error("处理流消息时出错", e))
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
        container.receive(
                Consumer.from(LOG_STATS_CONSUMER_GROUP, LOG_CONSUMER_NAME),
                StreamOffset.create(LOG_STATS_STREAM, ReadOffset.lastConsumed()),
                logStatsConsumer
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
    }*/
}
