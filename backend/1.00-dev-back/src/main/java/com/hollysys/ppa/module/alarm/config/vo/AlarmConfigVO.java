package com.hollysys.ppa.module.alarm.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报警配置详情 VO
 *
 * @author PPA Team
 */
@Data
@Schema(description = "报警配置详情")
public class AlarmConfigVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "专业ID")
    private Long specialtyId;

    @Schema(description = "系统ID")
    private Long systemId;

    @Schema(description = "测点ID")
    private Long measurePointId;

    @Schema(description = "报警类型")
    private Integer alarmType;

    @Schema(description = "报警等级")
    private Integer alarmLevel;

    @Schema(description = "启用状态")
    private Integer enabled;

    @Schema(description = "规则描述")
    private String description;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "规则明细列表")
    private List<AlarmRuleVO> rules;

    @Data
    @Schema(description = "规则明细项")
    public static class AlarmRuleVO {
        @Schema(description = "ID")
        private Long id;
        @Schema(description = "规则类型")
        private Integer ruleType;
        @Schema(description = "运算符")
        private String operator;
        @Schema(description = "阈值")
        private BigDecimal threshold;
        @Schema(description = "死区值")
        private BigDecimal deadband;
        @Schema(description = "速率周期")
        private Integer ratePeriod;
        @Schema(description = "持续时长")
        private Integer duration;
        @Schema(description = "逻辑表达式")
        private String logicExpression;
        @Schema(description = "排序号")
        private Integer sortOrder;
    }
}
