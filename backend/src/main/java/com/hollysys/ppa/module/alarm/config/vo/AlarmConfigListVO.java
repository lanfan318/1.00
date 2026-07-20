package com.hollysys.ppa.module.alarm.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报警配置列表 VO
 *
 * @author PPA Team
 */
@Data
@Schema(description = "报警配置列表项")
public class AlarmConfigListVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "机组名称")
    private String unitName;

    @Schema(description = "专业名称")
    private String specialtyName;

    @Schema(description = "系统名称")
    private String systemName;

    @Schema(description = "测点名称")
    private String measurePointName;

    @Schema(description = "报警类型")
    private Integer alarmType;

    @Schema(description = "报警等级")
    private Integer alarmLevel;

    @Schema(description = "启用状态")
    private Integer enabled;

    @Schema(description = "规则描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
