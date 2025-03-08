package com.timecold.shortlink.project.dto.biz;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShortLinkHistStatsDTO {
    private Long histPv;
    private Long histUv;
    private Long histUip;
}
