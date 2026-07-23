package com.hollysys.ppa.module.realtime.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 测点实时值 VO（前端卡片动态刷新用）
 */
@Data
@Schema(description = "测点实时值")
public class RealtimePointVO {

    @Schema(description = "测点ID")
    private Long pointId;

    @Schema(description = "测点编码")
    private String pointCode;

    @Schema(description = "测点名称")
    private String pointName;

    @Schema(description = "当前值")
    private BigDecimal currentValue;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "采样时间")
    private String sampleTime;

    @Schema(description = "质量标记")
    private Integer qualityFlag;

    @Schema(description = "变化趋势: up/down/stable")
    private String trend;

    @Schema(description = "变化量")
    private BigDecimal delta;

    @Schema(description = "是否异常")
    private Boolean anomaly;

    @Schema(description = "报警等级（有报警时才有值）")
    private Integer alarmLevel;

    @Schema(description = "健康评分（0-100）")
    private BigDecimal healthScore;
}
