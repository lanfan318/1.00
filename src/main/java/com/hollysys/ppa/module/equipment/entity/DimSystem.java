package com.hollysys.ppa.module.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统维度表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dim_system")
@Schema(description = "系统")
public class DimSystem extends BaseEntity {

    @Schema(description = "所属专业ID")
    private Long specialtyId;

    @Schema(description = "系统编码", example = "FGD")
    private String systemCode;

    @Schema(description = "系统名称", example = "脱硫系统")
    private String systemName;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
