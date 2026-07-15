package com.hollysys.ppa.module.alarm.realtime.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 报警分级表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_alarm_classify")
@Schema(description = "报警分级")
public class AlarmClassify extends BaseEntity {

    @Schema(description = "关联实时报警ID")
    private Long alarmId;

    @Schema(description = "分级等级: 1-一级 2-二级 3-智能预警")
    private Integer classifyLevel;

    @Schema(description = "分级依据")
    private String classifyBasis;

    @Schema(description = "分级时间")
    private LocalDateTime classifyTime;

    @Schema(description = "操作人")
    private String operator;
}
