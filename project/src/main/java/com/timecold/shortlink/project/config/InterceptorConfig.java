package com.timecold.shortlink.project.config;

import com.timecold.shortlink.project.common.interceptor.ShortLinkAccessInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private ShortLinkAccessInterceptor shortLinkAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(shortLinkAccessInterceptor)
                .addPathPatterns("/*");
    }
}
