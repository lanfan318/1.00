package com.hollysys.ppa.module.guide.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 引用溯源表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_reference_trace")
@Schema(description = "引用溯源")
public class ReferenceTrace extends BaseEntity {

    @Schema(description = "关联操作指导ID")
    private Long guideId;

    @Schema(description = "引用来源类型: 规程/历史案例/专家经验/文献")
    private String sourceType;

    @Schema(description = "引用来源名称")
    private String sourceName;

    @Schema(description = "引用来源路径或URL")
    private String sourceUrl;

    @Schema(description = "引用片段")
    private String snippet;
}
