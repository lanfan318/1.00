package com.hollysys.ppa.module.analysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "异常检测结果")
public class AnomalyResultVO {

    @Schema(description = "测点ID")
    private Long measurePointId;

    @Schema(description = "异常数据点数量")
    private Integer anomalyCount;

    @Schema(description = "异常率")
    private Double anomalyRate;

    @Schema(description = "异常数据点列表")
    private List<AnomalyPoint> points;

    @Data
    @Schema(description = "异常点")
    public static class AnomalyPoint {
        @Schema(description = "时间")
        private String timestamp;
        @Schema(description = "数值")
        private BigDecimal value;
        @Schema(description = "上界")
        private BigDecimal upperBound;
        @Schema(description = "下界")
        private BigDecimal lowerBound;
        @Schema(description = "偏离倍数")
        private Double deviationSigma;
    }
}
