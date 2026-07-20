package com.hollysys.ppa.module.alarm.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 报警统计概览 VO
 *
 * @author PPA Team
 */
@Data
@Schema(description = "报警统计概览")
public class AlarmStatsOverviewVO {

    @Schema(description = "报警总数")
    private Long totalCount;

    @Schema(description = "未处理数量")
    private Long unconfirmedCount;

    @Schema(description = "已抑制数量")
    private Long suppressedCount;

    @Schema(description = "平均处理时长（分钟）")
    private BigDecimal avgHandleMinutes;

    @Schema(description = "环比变化率（%）")
    private BigDecimal changeRate;

    @Schema(description = "统计开始时间")
    private String rangeStart;

    @Schema(description = "统计结束时间")
    private String rangeEnd;
}
