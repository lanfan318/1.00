package com.hollysys.ppa.module.alarm.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 报警配置分页查询 DTO
 *
 * @author PPA Team
 */
@Data
@Schema(description = "报警配置查询参数")
public class AlarmConfigQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "专业ID")
    private Long specialtyId;

    @Schema(description = "系统ID")
    private Long systemId;

    @Schema(description = "报警类型")
    private Integer alarmType;

    @Schema(description = "关键字（规则名称/编码）")
    private String keyword;
}
