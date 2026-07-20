package com.hollysys.ppa.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * AI 分析请求 DTO
 */
@Data
@Schema(description = "AI 分析请求")
public class AiAnalysisDTO {

    @Schema(description = "分析类型", example = "alarm_diagnosis")
    private String analysisType;

    @Schema(description = "报警ID")
    private Long alarmId;

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "额外参数")
    private Map<String, Object> extraParams;
}
