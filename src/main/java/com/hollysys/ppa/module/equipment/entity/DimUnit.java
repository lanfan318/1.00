package com.hollysys.ppa.module.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机组维度表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dim_unit")
@Schema(description = "机组")
public class DimUnit extends BaseEntity {

    @Schema(description = "机组编码", example = "10001")
    private String unitCode;

    @Schema(description = "机组名称", example = "1号机组")
    private String unitName;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
