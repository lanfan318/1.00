package com.hollysys.ppa.module.curve.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工况曲线 VO
 *
 * @author PPA Team
 */
@Data
@Schema(description = "工况曲线数据")
public class CurveDataVO {

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "曲线类型")
    private String curveType;

    @Schema(description = "数据点列表")
    private List<CurvePoint> points;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "曲线数据点")
    public static class CurvePoint {
        @Schema(description = "采样时间")
        private LocalDateTime time;
        @Schema(description = "采样值")
        private BigDecimal value;
        @Schema(description = "质量标记")
        private Integer qualityFlag;
    }
}
