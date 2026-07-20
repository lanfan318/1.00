package com.hollysys.ppa.module.alarm.suppress.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 报警抑制表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_alarm_suppress")
@Schema(description = "报警抑制")
public class AlarmSuppress extends BaseEntity {

    @Schema(description = "关联实时报警ID")
    private Long alarmId;

    @Schema(description = "报警配置ID")
    private Long configId;

    @Schema(description = "测点ID")
    private Long measurePointId;

    @Schema(description = "抑制类型: 1-手动抑制 2-自动抑制 3-定时抑制")
    private Integer suppressType;

    @Schema(description = "抑制原因")
    private String reason;

    @Schema(description = "抑制开始时间")
    private LocalDateTime startTime;

    @Schema(description = "抑制结束时间")
    private LocalDateTime endTime;

    @Schema(description = "操作人")
    private String operator;
}
