package com.timecold.shortlink.admin.biz.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户信息传输过滤器
 */

@Component
@RequiredArgsConstructor
public class UserTransmitInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws JsonProcessingException {

        // 验证 Token
        String username = request.getHeader("username");
        String uid = request.getHeader("uid");

        if (StringUtils.isNoneBlank(username, uid)) {
            UserInfoDTO userInfoDTO = new UserInfoDTO(Long.parseLong(uid), username);
            UserContext.setUser(userInfoDTO);
        }
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清理上下文
        UserContext.removeUser();
    }

}