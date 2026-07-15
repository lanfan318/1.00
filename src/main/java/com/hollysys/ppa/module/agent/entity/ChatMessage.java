package com.hollysys.ppa.module.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 消息表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_chat_message")
@Schema(description = "Agent 消息")
public class ChatMessage extends BaseEntity {

    @Schema(description = "所属会话ID")
    private Long sessionId;

    @Schema(description = "角色: user/assistant/system")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "Agent 服务返回的原始数据（JSON）")
    private String rawData;

    @Schema(description = "消息序号")
    private Integer seq;
}
