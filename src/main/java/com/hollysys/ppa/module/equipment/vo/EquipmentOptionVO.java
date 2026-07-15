package com.hollysys.ppa.module.equipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备下拉选项 VO
 *
 * @author PPA Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备下拉选项")
public class EquipmentOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "父级ID（系统所属专业ID、测点所属系统ID 等）")
    private Long parentId;

    @Schema(description = "扩展字段（测点类型/单位等）")
    private String extra;

    public EquipmentOptionVO(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public EquipmentOptionVO(Long id, String code, String name, Long parentId) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.parentId = parentId;
    }
}
