package com.timecold.shortlink.project.config;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAgentAnalyzerConfig {

    private static final int CACHE_SIZE = 1000;

    @Bean
    public UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer
                .newBuilder()
                .withFields(UserAgent.DEVICE_CLASS, UserAgent.OPERATING_SYSTEM_NAME, UserAgent.AGENT_NAME)
                .withCache(CACHE_SIZE)
                .build();
    }
}

