package com.hollysys.ppa.module.analysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Schema(description = "健康评分计算结果")
public class HealthCalcResultVO {

    @Schema(description = "综合评分")
    private BigDecimal totalScore;

    @Schema(description = "等级: excellent/good/fair/poor/critical")
    private String grade;

    @Schema(description = "各维度评分")
    private Map<String, BigDecimal> dimensionScores;

    @Schema(description = "各维度权重")
    private Map<String, Double> weights;

    @Schema(description = "改进建议")
    private String suggestion;
}
