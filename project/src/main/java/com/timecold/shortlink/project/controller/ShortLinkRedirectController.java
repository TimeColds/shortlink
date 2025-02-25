package com.timecold.shortlink.project.controller;

import com.timecold.shortlink.project.service.ShortLinkService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShortLinkRedirectController {

    private final ShortLinkService shortLinkService;


    /**
     * 短链接跳转
     */
    @GetMapping(value = "/{short-uri}")
    public void redirectUrl(@PathVariable("short-uri") String shortUrl, HttpServletResponse response) {
        shortLinkService.redirectUrl(shortUrl, response);
    }
}
