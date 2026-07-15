package com.hollysys.ppa.module.alarm.realtime.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实时报警查询 DTO
 *
 * @author PPA Team
 */
@Data
@Schema(description = "实时报警查询参数")
public class AlarmRealtimeQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "报警等级")
    private Integer alarmLevel;

    @Schema(description = "报警类型")
    private Integer alarmType;

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "专业ID")
    private Long specialtyId;

    @Schema(description = "确认状态: 0-未确认 1-已确认")
    private Integer confirmed;

    @Schema(description = "关键字（标题/编码）")
    private String keyword;
}
