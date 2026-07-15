package com.hollysys.ppa.module.alarm.realtime.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 报警分级分组 VO
 *
 * @author PPA Team
 */
@Data
@Schema(description = "报警分级分组")
public class AlarmLevelGroupVO {

    @Schema(description = "一级报警")
    private LevelGroup level1;

    @Schema(description = "二级报警")
    private LevelGroup level2;

    @Schema(description = "智能预警")
    private LevelGroup level3;

    @Data
    @Schema(description = "等级分组详情")
    public static class LevelGroup {
        @Schema(description = "报警等级")
        private Integer level;
        @Schema(description = "报警数量")
        private Long count;
        @Schema(description = "最新报警列表（Top 5）")
        private List<AlarmRealtimeVO> topList;
    }
}
