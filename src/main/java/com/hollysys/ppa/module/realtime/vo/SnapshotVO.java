package com.hollysys.ppa.module.realtime.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 机组快照 VO（仪表盘总览用）
 */
@Data
@Schema(description = "机组实时快照")
public class SnapshotVO {

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "机组名称")
    private String unitName;

    @Schema(description = "测点总数")
    private Integer totalPoints;

    @Schema(description = "在线测点数")
    private Integer onlinePoints;

    @Schema(description = "异常测点数")
    private Integer anomalyPoints;

    @Schema(description = "报警数")
    private Integer alarmCount;

    @Schema(description = "整体健康评分")
    private Double overallHealth;

    @Schema(description = "测点实时值列表")
    private List<RealtimePointVO> points;
}
