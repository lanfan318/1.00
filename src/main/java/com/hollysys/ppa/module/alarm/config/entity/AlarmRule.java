package com.hollysys.ppa.module.alarm.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 报警规则明细表（一对多子表）
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_alarm_rule")
@Schema(description = "报警规则明细")
public class AlarmRule extends BaseEntity {

    @Schema(description = "所属报警配置ID")
    private Long configId;

    @Schema(description = "规则类型: 1-阈值 2-速率 3-偏差 4-逻辑")
    private Integer ruleType;

    @Schema(description = "比较运算符: GT/GTE/LT/LTE/EQ/NEQ")
    private String operator;

    @Schema(description = "阈值")
    private BigDecimal threshold;

    @Schema(description = "死区值")
    private BigDecimal deadband;

    @Schema(description = "速率周期（秒），速率预警时使用")
    private Integer ratePeriod;

    @Schema(description = "持续时长（秒），防抖用")
    private Integer duration;

    @Schema(description = "逻辑表达式，逻辑预警时使用")
    private String logicExpression;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
