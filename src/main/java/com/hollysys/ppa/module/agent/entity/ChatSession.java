package com.hollysys.ppa.module.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 会话表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_chat_session")
@Schema(description = "Agent 会话")
public class ChatSession extends BaseEntity {

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "会话状态: active/closed")
    private String status;

    @Schema(description = "创建人")
    private String createdBy;
}
