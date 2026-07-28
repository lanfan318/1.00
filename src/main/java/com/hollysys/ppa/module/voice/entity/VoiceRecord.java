package com.hollysys.ppa.module.voice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 语音识别记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_voice_record")
@Schema(description = "语音识别记录")
public class VoiceRecord extends BaseEntity {

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "音频文件URL")
    private String audioUrl;

    @Schema(description = "音频时长（毫秒）")
    private Long audioDurationMs;

    @Schema(description = "音频文件大小（字节）")
    private Long audioSizeBytes;

    @Schema(description = "转录文本")
    private String transcriptText;

    @Schema(description = "识别置信度")
    private Double confidence;

    @Schema(description = "意图: query_alarm/confirm_alarm/snapshot/health/chat/unknown")
    private String intent;

    @Schema(description = "意图参数 JSON")
    private String intentParams;

    @Schema(description = "引擎类型: mock/aliyun_nls/iflytek")
    private String engineType;

    @Schema(description = "引擎版本")
    private String engineVersion;

    @Schema(description = "识别耗时（毫秒）")
    private Long costMs;

    @Schema(description = "状态: 1-成功 0-失败")
    private Integer status;

    @Schema(description = "错误信息")
    private String errorMsg;
}
