package com.hollysys.ppa.module.curve.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测点时序数据表（TimescaleDB hypertable）
 *
 * @author PPA Team
 */
@Data
@TableName("ts_measure_point")
@Schema(description = "测点时序数据")
public class TsMeasurePoint implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "采样时间（分区键）")
    private LocalDateTime ts;

    @Schema(description = "测点ID")
    private Long measurePointId;

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "采样值")
    private BigDecimal value;

    @Schema(description = "质量标记: 0-正常 1-可疑 2-坏值")
    private Integer qualityFlag;
}
