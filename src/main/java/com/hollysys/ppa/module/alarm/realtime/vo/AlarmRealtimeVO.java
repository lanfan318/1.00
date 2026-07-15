package com.hollysys.ppa.module.alarm.realtime.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时报警列表 VO
 *
 * @author PPA Team
 */
@Data
@Schema(description = "实时报警列表项")
public class AlarmRealtimeVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "报警编码")
    private String alarmCode;

    @Schema(description = "报警标题")
    private String alarmTitle;

    @Schema(description = "报警类型")
    private Integer alarmType;

    @Schema(description = "报警等级")
    private Integer alarmLevel;

    @Schema(description = "机组名称")
    private String unitName;

    @Schema(description = "专业名称")
    private String specialtyName;

    @Schema(description = "系统名称")
    private String systemName;

    @Schema(description = "测点名称")
    private String measurePointName;

    @Schema(description = "当前值")
    private BigDecimal currentValue;

    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    @Schema(description = "报警发生时间")
    private LocalDateTime occurTime;

    @Schema(description = "确认状态: 0-未确认 1-已确认")
    private Integer confirmed;

    @Schema(description = "确认人")
    private String confirmBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "抑制状态: 0-正常 1-已抑制")
    private Integer suppressed;

    @Schema(description = "分级标签")
    private Integer classifyLevel;
}
