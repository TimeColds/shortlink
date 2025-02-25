package com.timecold.shortlink.admin.biz.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.timecold.shortlink.admin.common.constant.RedisKeyConstant;
import com.timecold.shortlink.admin.common.convention.exception.ClientException;
import com.timecold.shortlink.admin.common.enums.UserErrorCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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

    private final ObjectMapper objectMapper;

    private static final List<String> IGNORE_URL = Lists.newArrayList(
            "/api/v1/short_link/admin/user/login",
            "/api/v1/short_link/admin/user/has-username"
    );
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws JsonProcessingException {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 忽略特定 URL
        if (IGNORE_URL.contains(requestURI)) {
            return true;
        }

        // 忽略注册请求
        if (requestURI.equals("/api/v1/short_link/admin/user") && method.equals("POST")) {
            return true;
        }

        // 验证 Token
        String username = request.getHeader("username");
        String token = request.getHeader("token");

        if (StringUtils.isAnyBlank(username, token)) {
            throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
        }

        // 从 Redis 验证 Token
        String userInfoJsonStr = (String)stringRedisTemplate.opsForHash().get(RedisKeyConstant.USER_LOGIN_KEY + username, token);
        if (userInfoJsonStr == null) {
            throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
        }

        // 存储用户信息到上下文
        UserInfoDTO userInfoDTO = objectMapper.readValue(userInfoJsonStr, UserInfoDTO.class);
        UserContext.setUser(userInfoDTO);
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清理上下文
        UserContext.removeUser();
    }

}