package com.timecold.shortlink.admin.config;

import com.timecold.shortlink.admin.biz.user.UserTransmitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用户配置自动装配
 */
@Configuration
public class UserConfiguration implements WebMvcConfigurer {

    /**
     * 用户信息传递过滤器
     */
    @Autowired
    private UserTransmitInterceptor userTransmitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userTransmitInterceptor)
                .addPathPatterns("/**") // 拦截所有路径
                .excludePathPatterns("/static/**"); // 排除静态资源
    }
}
