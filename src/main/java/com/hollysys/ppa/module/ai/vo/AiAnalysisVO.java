package com.hollysys.ppa.module.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 分析结果 VO
 */
@Data
@Schema(description = "AI 分析结果")
public class AiAnalysisVO {

    @Schema(description = "分析ID")
    private Long id;

    @Schema(description = "分析类型")
    private String analysisType;

    @Schema(description = "输出结果")
    private Object outputResult;

    @Schema(description = "置信度")
    private Double confidence;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "分析耗时（ms）")
    private Long costMs;

    @Schema(description = "分析时间")
    private LocalDateTime createdAt;
}
