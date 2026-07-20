package com.hollysys.ppa.module.analysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "趋势预测结果")
public class TrendResultVO {

    @Schema(description = "测点ID")
    private Long measurePointId;

    @Schema(description = "斜率（每小时变化量）")
    private BigDecimal slope;

    @Schema(description = "截距")
    private BigDecimal intercept;

    @Schema(description = "拟合优度 R²")
    private Double rSquared;

    @Schema(description = "趋势方向: up/down/stable")
    private String direction;

    @Schema(description = "预测数据点")
    private List<PredictPoint> predictions;

    @Data
    @Schema(description = "预测点")
    public static class PredictPoint {
        @Schema(description = "时间")
        private String timestamp;
        @Schema(description = "预测值")
        private BigDecimal value;
    }
}
