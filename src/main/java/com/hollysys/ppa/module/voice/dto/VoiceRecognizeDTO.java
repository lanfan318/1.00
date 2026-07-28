package com.hollysys.ppa.module.voice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 语音识别请求
 */
@Data
@Schema(description = "语音识别请求")
public class VoiceRecognizeDTO {

    @Schema(description = "会话ID，不传则自动创建")
    private Long sessionId;

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "引擎类型: mock/aliyun_nls/iflytek，默认 mock")
    private String engineType;

    @Schema(description = "是否执行语音指令（识别后自动执行操作）")
    private Boolean executeCommand;
}
