package com.hollysys.ppa.module.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * AI 对话请求 DTO
 */
@Data
@Schema(description = "AI 对话请求")
public class ChatRequestDTO {

    @NotNull(message = "会话ID不能为空")
    @Schema(description = "会话ID")
    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户消息内容")
    private String message;

    @Schema(description = "附加参数（可选）")
    private Map<String, Object> params;
}
