package com.hollysys.ppa.module.analysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "报警关联分析结果")
public class CorrelationResultVO {

    @Schema(description = "源报警ID")
    private Long sourceAlarmId;

    @Schema(description = "关联报警列表")
    private List<CorrelatedAlarm> correlatedAlarms;

    @Schema(description = "时间窗口（分钟）")
    private Integer windowMinutes;

    @Data
    @Schema(description = "关联报警")
    public static class CorrelatedAlarm {
        @Schema(description = "报警ID")
        private Long alarmId;
        @Schema(description = "报警标题")
        private String alarmTitle;
        @Schema(description = "时间差（秒）")
        private Long timeDelta;
        @Schema(description = "关联系数")
        private Double correlationScore;
        @Schema(description = "共现次数")
        private Integer coOccurrence;
    }
}
