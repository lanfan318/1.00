package com.hollysys.ppa.module.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 专业维度表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dim_specialty")
@Schema(description = "专业")
public class DimSpecialty extends BaseEntity {

    @Schema(description = "专业编码", example = "BOILER")
    private String specialtyCode;

    @Schema(description = "专业名称", example = "锅炉")
    private String specialtyName;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
