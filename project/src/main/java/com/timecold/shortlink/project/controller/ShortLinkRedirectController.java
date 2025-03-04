package com.timecold.shortlink.project.controller;

import com.timecold.shortlink.project.service.ShortLinkService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class ShortLinkRedirectController {

    private final ShortLinkService shortLinkService;


    /**
     * 短链接跳转
     */
    @GetMapping(value = "/{short-uri}")
    public void redirectUrl(@PathVariable("short-uri") String shortUrl, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("favicon.ico".equals(shortUrl)) {
            return;
        }
        String redirectUrl = shortLinkService.redirectUrl(shortUrl);
        if ("notFound".equals(redirectUrl)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            request.getRequestDispatcher("/error/notfound.html").forward(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", redirectUrl);
    }
}
