package com.hollysys.ppa.module.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 设备预测结果 VO
 */
@Data
@Schema(description = "设备预测结果")
public class PredictResultVO {

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "当前健康评分")
    private BigDecimal currentScore;

    @Schema(description = "预测健康评分（7天后）")
    private BigDecimal predictedScore;

    @Schema(description = "恶化速率（分/天）")
    private BigDecimal degradationRate;

    @Schema(description = "预计剩余健康天数")
    private Long remainingDays;

    @Schema(description = "风险等级: low/medium/high/critical")
    private String riskLevel;

    @Schema(description = "关键影响因子")
    private List<FactorVO> factors;

    @Data
    @Schema(description = "影响因子")
    public static class FactorVO {
        @Schema(description = "因子名称")
        private String name;
        @Schema(description = "贡献度")
        private Double contribution;
    }
}
