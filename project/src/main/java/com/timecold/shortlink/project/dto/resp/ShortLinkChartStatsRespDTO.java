package com.timecold.shortlink.project.dto.resp;


import lombok.Data;

import java.util.List;

@Data
public class ShortLinkChartStatsRespDTO {

    private List<Long> weekdayStats;
    private List<Long> hourStats;
    private List<OsStats> osStats;
    private List<BrowserStats> browserStats;
    private List<DeviceStats> deviceStats;
    private List<LocationStats> locationStats;
    private Integer visitorType;

    @Data
    public static class OsStats{
        private String os;
        private Long count;
        private Double ratio;
    }
    @Data
    public static class BrowserStats {
        private String browser;
        private Long count;
        private Double ratio;
    }
    @Data
    public static class DeviceStats {
        private String device;
        private Long count;
        private Double ratio;
    }
    @Data
    public static class LocationStats {
        private String location;
        private Long count;
        private Double ratio;
    }

}
