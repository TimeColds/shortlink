package com.timecold.shortlink.project.common.interceptor;

import com.timecold.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.timecold.shortlink.project.service.ShortLinkAnalyticsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkAccessInterceptor implements HandlerInterceptor {

    private final ShortLinkAnalyticsService shortLinkAnalyticsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        String shortUrl = request.getRequestURI().substring(1);
        if ("favicon.ico".equals(shortUrl)) {
            return;
        }
        String referer = request.getHeader("Referer");
        if (referer == null) {
            referer = "直接访问";
        }
        ShortLinkStatsRecordDTO shortLinkStatsRecordDTO = ShortLinkStatsRecordDTO.builder()
                .shortUrl(shortUrl)
                .userIdentifier(getUserIdentifier(request, response))
                .accessTime(LocalDateTime.now())
                .ip(getClientIp(request))
                .referer(referer)
                .userAgent(request.getHeader("User-Agent"))
                .build();
        shortLinkAnalyticsService.processAccess(shortLinkStatsRecordDTO);
    }

    private String getUserIdentifier(HttpServletRequest request, HttpServletResponse response) {
        String cookieName = "tm_v";

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String tmV = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(cookieName, tmV);
        cookie.setMaxAge(60 * 60 * 24 * 30);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        return tmV;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
