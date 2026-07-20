package com.hollysys.ppa.module.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测点维度表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dim_measure_point")
@Schema(description = "测点")
public class DimMeasurePoint extends BaseEntity {

    @Schema(description = "所属机组ID")
    private Long unitId;

    @Schema(description = "所属系统ID")
    private Long systemId;

    @Schema(description = "测点编码（机组号:位号:类型）", example = "10001:MALF114:IN")
    private String pointCode;

    @Schema(description = "测点名称", example = "主蒸汽温度")
    private String pointName;

    @Schema(description = "位号", example = "MALF114")
    private String tagNo;

    @Schema(description = "类型（IN/OUT/MID）", example = "IN")
    private String pointType;

    @Schema(description = "单位", example = "℃")
    private String unit;

    @Schema(description = "量程下限")
    private Double rangeLow;

    @Schema(description = "量程上限")
    private Double rangeHigh;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
