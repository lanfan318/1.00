package com.hollysys.ppa.module.alarm.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日趋势 VO
 *
 * @author PPA Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "日趋势项")
public class AlarmStatsTrendVO {

    @Schema(description = "日期", example = "2026-07-01")
    private String date;

    @Schema(description = "一级数量")
    private Long level1Count;

    @Schema(description = "二级数量")
    private Long level2Count;

    @Schema(description = "智能预警数量")
    private Long level3Count;

    @Schema(description = "总数量")
    private Long totalCount;
}
