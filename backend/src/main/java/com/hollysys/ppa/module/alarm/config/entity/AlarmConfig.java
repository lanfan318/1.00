package com.hollysys.ppa.module.alarm.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报警配置表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_alarm_config")
@Schema(description = "报警配置")
public class AlarmConfig extends BaseEntity {

    @Schema(description = "规则名称", example = "主蒸汽温度高报警")
    private String ruleName;

    @Schema(description = "规则编码", example = "ALM-001")
    private String ruleCode;

    @Schema(description = "所属机组ID")
    private Long unitId;

    @Schema(description = "所属专业ID")
    private Long specialtyId;

    @Schema(description = "所属系统ID")
    private Long systemId;

    @Schema(description = "关联测点ID")
    private Long measurePointId;

    @Schema(description = "报警类型: 1-预测预警 2-故障预警 3-阈值预警 4-视频预警 5-速率预警")
    private Integer alarmType;

    @Schema(description = "报警等级: 1-一级 2-二级 3-智能预警")
    private Integer alarmLevel;

    @Schema(description = "启用状态: 1-启用 0-禁用")
    private Integer enabled;

    @Schema(description = "规则描述")
    private String description;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
