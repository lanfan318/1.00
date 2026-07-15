package com.hollysys.ppa.module.guide.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识图谱推理记录表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_kg_reasoning")
@Schema(description = "知识图谱推理")
public class KgReasoning extends BaseEntity {

    @Schema(description = "关联报警ID")
    private Long alarmId;

    @Schema(description = "推理路径（JSON）")
    private String reasoningPath;

    @Schema(description = "置信度")
    private Double confidence;

    @Schema(description = "KG 服务返回的原始结果（JSON）")
    private String rawResult;

    @Schema(description = "推理耗时（毫秒）")
    private Long costMs;
}
