package com.hollysys.ppa.module.alarm.realtime.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报警历史表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_alarm_history")
@Schema(description = "报警历史")
public class AlarmHistory extends BaseEntity {

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

    @Schema(description = "峰值")
    private BigDecimal peakValue;

    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    @Schema(description = "发生时间")
    private LocalDateTime occurTime;

    @Schema(description = "恢复时间")
    private LocalDateTime recoverTime;

    @Schema(description = "持续时长（秒）")
    private Long durationSeconds;

    @Schema(description = "确认人")
    private String confirmBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "处理状态")
    private Integer handleStatus;

    @Schema(description = "处理备注")
    private String handleRemark;
}
