package com.hollysys.ppa.module.alarm.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报警配置保存 DTO（含多行规则子表）
 *
 * @author PPA Team
 */
@Data
@Schema(description = "报警配置新增/编辑请求")
public class AlarmConfigSaveDTO {

    @NotBlank(message = "规则名称不能为空")
    @Schema(description = "规则名称", example = "主蒸汽温度高报警")
    private String ruleName;

    @NotBlank(message = "规则编码不能为空")
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

    @NotNull(message = "报警类型不能为空")
    @Schema(description = "报警类型")
    private Integer alarmType;

    @NotNull(message = "报警等级不能为空")
    @Schema(description = "报警等级")
    private Integer alarmLevel;

    @Schema(description = "启用状态")
    private Integer enabled = 1;

    @Schema(description = "规则描述")
    private String description;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "规则明细子表")
    private List<AlarmRuleDTO> rules;

    @Data
    @Schema(description = "规则明细项")
    public static class AlarmRuleDTO {

        @NotNull(message = "规则类型不能为空")
        @Schema(description = "规则类型: 1-阈值 2-速率 3-偏差 4-逻辑")
        private Integer ruleType;

        @Schema(description = "比较运算符: GT/GTE/LT/LTE/EQ/NEQ")
        private String operator;

        @Schema(description = "阈值")
        private BigDecimal threshold;

        @Schema(description = "死区值")
        private BigDecimal deadband;

        @Schema(description = "速率周期（秒）")
        private Integer ratePeriod;

        @Schema(description = "持续时长（秒）")
        private Integer duration;

        @Schema(description = "逻辑表达式")
        private String logicExpression;

        @Schema(description = "排序号")
        private Integer sortOrder;
    }
}
