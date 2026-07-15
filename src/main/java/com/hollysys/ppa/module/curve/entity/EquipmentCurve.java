package com.hollysys.ppa.module.curve.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工况曲线表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_equipment_curve")
@Schema(description = "工况曲线")
public class EquipmentCurve extends BaseEntity {

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "测点ID")
    private Long measurePointId;

    @Schema(description = "曲线类型: fd-风道 sb-设备")
    private String curveType;

    @Schema(description = "采样时间")
    private LocalDateTime sampleTime;

    @Schema(description = "采样值")
    private BigDecimal sampleValue;

    @Schema(description = "质量标记: 0-正常 1-可疑 2-坏值")
    private Integer qualityFlag;
}
