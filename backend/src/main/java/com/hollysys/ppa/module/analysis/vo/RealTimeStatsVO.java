package com.hollysys.ppa.module.analysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "实时统计数据")
public class RealTimeStatsVO {

    @Schema(description = "测点ID")
    private Long measurePointId;

    @Schema(description = "测点名称")
    private String pointName;

    @Schema(description = "当前值")
    private BigDecimal currentValue;

    @Schema(description = "平均值")
    private BigDecimal avgValue;

    @Schema(description = "最大值")
    private BigDecimal maxValue;

    @Schema(description = "最小值")
    private BigDecimal minValue;

    @Schema(description = "标准差")
    private BigDecimal stdDev;

    @Schema(description = "数据点数")
    private Long count;

    @Schema(description = "变化率")
    private BigDecimal changeRate;
}
