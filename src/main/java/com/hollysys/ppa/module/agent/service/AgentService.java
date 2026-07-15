package com.hollysys.ppa.module.agent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.module.agent.entity.ChatMessage;
import com.hollysys.ppa.module.agent.entity.ChatSession;

import java.util.List;

/**
 * Agent 会话 Service
 *
 * @author PPA Team
 */
public interface AgentService {

    Page<ChatSession> listSessions(Integer page, Integer size);

    List<ChatMessage> getMessages(Long sessionId);

    ChatSession createSession(String title, String createdBy);
}
