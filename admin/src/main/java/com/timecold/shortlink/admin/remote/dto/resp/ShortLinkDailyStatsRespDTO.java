package com.timecold.shortlink.admin.remote.dto.resp;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ShortLinkDailyStatsRespDTO {

    private StatsAll statsAll;

    private List<DailyStats> dailyStats;

    @Data
    public static class StatsAll{
        private Long pv;
        private Long uv;
        private Long uip;
    }

    @Data
    public static class DailyStats {
        private LocalDate date;

        private Long pv;

        private Long uv;

        private Long uip;
    }
}
