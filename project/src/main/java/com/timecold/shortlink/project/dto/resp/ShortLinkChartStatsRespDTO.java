package com.timecold.shortlink.project.dto.resp;


import lombok.Data;

import java.util.List;

@Data
public class ShortLinkChartStatsRespDTO {

    private List<Long> weekdayStats;
    private List<Long> hourStats;
    private List<ProvinceStats> provinceStats;
    private List<BrowserStats> browserStats;
    private List<OsStats> osStats;
    private List<DeviceStats> deviceStats;
    private Long newVisitor;

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
    public static class ProvinceStats {
        private String province;
        private Long count;
        private Double ratio;
    }

}
