package com.hollysys.ppa.module.alarm.realtime.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时报警表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_alarm_realtime")
@Schema(description = "实时报警")
public class AlarmRealtime extends BaseEntity {

    @Schema(description = "关联报警配置ID")
    private Long configId;

    @Schema(description = "报警编码")
    private String alarmCode;

    @Schema(description = "报警标题")
    private String alarmTitle;

    @Schema(description = "报警类型")
    private Integer alarmType;

    @Schema(description = "报警等级")
    private Integer alarmLevel;

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "专业ID")
    private Long specialtyId;

    @Schema(description = "系统ID")
    private Long systemId;

    @Schema(description = "测点ID")
    private Long measurePointId;

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

    @Schema(description = "抑制原因")
    private String suppressReason;

    @Schema(description = "分级标签: 1-一级 2-二级 3-智能预警")
    private Integer classifyLevel;
}
