package com.hollysys.ppa.module.voice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 语音识别记录 VO
 */
@Data
@Schema(description = "语音识别记录")
public class VoiceRecordVO {

    private Long id;
    private Long sessionId;
    private String transcriptText;
    private Double confidence;
    private String intent;
    private Object intentResult;
    private String engineType;
    private Long audioDurationMs;
    private Long costMs;
    private LocalDateTime createdAt;
}
