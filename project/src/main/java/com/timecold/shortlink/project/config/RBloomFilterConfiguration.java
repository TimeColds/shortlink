package com.timecold.shortlink.project.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RBloomFilterConfiguration {


    /**
     * 防止短链接查询数据库的布隆过滤器
     */
    @Bean
    public RBloomFilter<String> shortUrlRegisterCachePenetrationBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter("shortUrlCachePenetrationBloomFilter");
        cachePenetrationBloomFilter.tryInit(100000000L, 0.001);
        return cachePenetrationBloomFilter;
    }
}
