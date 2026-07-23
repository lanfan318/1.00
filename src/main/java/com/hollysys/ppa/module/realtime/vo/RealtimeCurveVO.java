package com.hollysys.ppa.module.realtime.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 实时曲线 VO（前端 ECharts 动态刷新用）
 */
@Data
@Schema(description = "实时曲线数据")
public class RealtimeCurveVO {

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "曲线类型")
    private String curveType;

    @Schema(description = "最新数据点列表")
    private List<CurvePoint> points;

    @Schema(description = "统计摘要")
    private CurveStats stats;

    @Schema(description = "异常检测结果")
    private AnomalyInfo anomalyInfo;

    @Data
    @Schema(description = "曲线数据点")
    public static class CurvePoint {
        @Schema(description = "时间戳")
        private String timestamp;
        @Schema(description = "数值")
        private BigDecimal value;
    }

    @Data
    @Schema(description = "曲线统计")
    public static class CurveStats {
        private BigDecimal min;
        private BigDecimal max;
        private BigDecimal avg;
        private String trend;
    }

    @Data
    @Schema(description = "异常信息")
    public static class AnomalyInfo {
        private Boolean hasAnomaly;
        private Integer anomalyCount;
        private String description;
    }
}
