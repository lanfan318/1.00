package com.hollysys.ppa.module.agent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.module.agent.dto.ChatRequestDTO;
import com.hollysys.ppa.module.agent.entity.ChatMessage;
import com.hollysys.ppa.module.agent.entity.ChatSession;
import com.hollysys.ppa.module.agent.vo.ChatReplyVO;

import java.util.List;

/**
 * Agent 会话 Service
 */
public interface AgentService {

    Page<ChatSession> listSessions(Integer page, Integer size);

    List<ChatMessage> getMessages(Long sessionId);

    ChatSession createSession(String title, String createdBy);

    /**
     * AI 对话代理：落库用户消息 → 转发 Agent 服务 → 落库 AI 回复 → 返回
     */
    ChatReplyVO chat(ChatRequestDTO dto);
}
