package com.hollysys.ppa.module.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 对话回复 VO
 */
@Data
@Schema(description = "AI 对话回复")
public class ChatReplyVO {

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "用户消息ID")
    private Long userMessageId;

    @Schema(description = "AI 回复消息ID")
    private Long assistantMessageId;

    @Schema(description = "AI 回复内容")
    private String content;

    @Schema(description = "AI 服务原始返回（JSON）")
    private String rawData;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;
}
