package com.timecold.shortlink.admin.biz.user;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.timecold.shortlink.admin.common.convention.exception.ClientException;
import com.timecold.shortlink.admin.common.enums.UserErrorCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 用户信息传输过滤器
 */

@Component
@RequiredArgsConstructor
public class UserTransmitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final List<String> IGNORE_URL = Lists.newArrayList(
            "/api/short-link/admin/v1/user/login",
            "/api/short-link/admin/v1/user/has-username"
    );
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 忽略特定 URL
        if (IGNORE_URL.contains(requestURI)) {
            return true;
        }

        // 忽略注册请求
        if (requestURI.equals("/api/short-link/admin/v1/user") && "POST".equals(method)) {
            return true;
        }

        // 验证 Token
        String username = request.getHeader("username");
        String token = request.getHeader("token");
        if (!StrUtil.isAllNotBlank(username, token)) {
            throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
        }

        // 从 Redis 验证 Token
        Object userInfoJsonStr = stringRedisTemplate.opsForHash().get("login_" + username, token);
        if (userInfoJsonStr == null) {
            throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
        }

        // 存储用户信息到上下文
        UserInfoDTO userInfoDTO = JSON.parseObject(userInfoJsonStr.toString(), UserInfoDTO.class);
        UserContext.setUser(userInfoDTO);
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清理上下文
        UserContext.removeUser();
    }

}