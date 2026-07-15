package com.hollysys.ppa.module.alarm.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 报警排名 VO（频次 / 时长 Top10）
 *
 * @author PPA Team
 */
@Data
@Schema(description = "报警排名项")
public class AlarmStatsRankVO {

    @Schema(description = "报警标题")
    private String alarmTitle;

    @Schema(description = "报警类型")
    private Integer alarmType;

    @Schema(description = "专业名称")
    private String specialtyName;

    @Schema(description = "频次")
    private Long frequency;

    @Schema(description = "平均持续时长（秒）")
    private BigDecimal avgDurationSeconds;

    @Schema(description = "总持续时长（秒）")
    private Long totalDurationSeconds;
}
