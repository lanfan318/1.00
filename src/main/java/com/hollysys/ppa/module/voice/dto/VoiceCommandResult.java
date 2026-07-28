package com.hollysys.ppa.module.voice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 语音指令执行结果
 */
@Data
@Schema(description = "语音指令执行结果")
public class VoiceCommandResult {

    @Schema(description = "识别意图")
    private String intent;

    @Schema(description = "操作描述")
    private String description;

    @Schema(description = "执行参数")
    private Map<String, Object> params;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "错误信息")
    private String errorMsg;
}
