package com.hollysys.ppa.module.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 根因分析结果 VO
 */
@Data
@Schema(description = "根因分析结果")
public class RootCauseVO {

    @Schema(description = "报警ID")
    private Long alarmId;

    @Schema(description = "根因列表")
    private List<CauseItem> rootCauses;

    @Schema(description = "关联报警")
    private List<Long> relatedAlarmIds;

    @Schema(description = "因果链描述")
    private String causalChain;

    @Schema(description = "置信度")
    private Double confidence;

    @Schema(description = "建议措施")
    private String recommendation;

    @Data
    @Schema(description = "根因项")
    public static class CauseItem {
        @Schema(description = "原因描述")
        private String cause;
        @Schema(description = "概率")
        private Double probability;
        @Schema(description = "证据")
        private List<String> evidences;
    }
}
