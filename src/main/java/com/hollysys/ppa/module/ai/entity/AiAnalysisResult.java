package com.hollysys.ppa.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 分析结果记录表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_analysis_result")
@Schema(description = "AI 分析结果")
public class AiAnalysisResult extends BaseEntity {

    @Schema(description = "关联报警ID")
    private Long alarmId;

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "分析类型: alarm_diagnosis/equipment_predict/anomaly_detect/root_cause")
    private String analysisType;

    @Schema(description = "输入数据 JSON")
    private String inputData;

    @Schema(description = "输出结果 JSON")
    private String outputResult;

    @Schema(description = "置信度")
    private Double confidence;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "分析耗时（毫秒）")
    private Long costMs;

    @Schema(description = "状态: 1-成功 0-失败")
    private Integer status;

    @Schema(description = "错误信息")
    private String errorMsg;
}
