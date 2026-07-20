package com.hollysys.ppa.module.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.module.agent.entity.ChatMessage;
import com.hollysys.ppa.module.agent.entity.ChatSession;
import com.hollysys.ppa.module.agent.mapper.ChatMessageMapper;
import com.hollysys.ppa.module.agent.mapper.ChatSessionMapper;
import com.hollysys.ppa.module.agent.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent 会话 Service 实现
 *
 * @author PPA Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    @Override
    public Page<ChatSession> listSessions(Integer page, Integer size) {
        Page<ChatSession> p = new Page<>(page, size);
        return sessionMapper.selectPage(p,
                new LambdaQueryWrapper<ChatSession>()
                        .orderByDesc(ChatSession::getCreatedAt));
    }

    @Override
    public List<ChatMessage> getMessages(Long sessionId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getSeq));
    }

    @Override
    public ChatSession createSession(String title, String createdBy) {
        ChatSession session = new ChatSession();
        session.setTitle(title);
        session.setStatus("active");
        session.setCreatedBy(createdBy);
        sessionMapper.insert(session);
        log.info("创建 Agent 会话: id={}, title={}", session.getId(), title);
        return session;
    }
}
