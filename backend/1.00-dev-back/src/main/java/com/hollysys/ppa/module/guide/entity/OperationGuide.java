package com.hollysys.ppa.module.guide.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作指导表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_operation_guide")
@Schema(description = "操作指导")
public class OperationGuide extends BaseEntity {

    @Schema(description = "关联报警ID")
    private Long alarmId;

    @Schema(description = "指导标题")
    private String title;

    @Schema(description = "指导内容（富文本）")
    private String content;

    @Schema(description = "诊断结论")
    private String diagnosis;

    @Schema(description = "建议措施")
    private String suggestion;

    @Schema(description = "风险等级: 高/中/低")
    private String riskLevel;

    @Schema(description = "运行意见")
    private String comment;

    @Schema(description = "运行意见填写人")
    private String commentBy;

    @Schema(description = "来源: 1-知识图谱 2-Agent 3-人工")
    private Integer source;
}
