package com.timecold.shortlink.project.service;

import com.timecold.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;

/**
 * 短链接分析接口层
 */
public interface ShortLinkAnalyticsService {

    void processAccess(ShortLinkStatsRecordDTO requestParam);
}
