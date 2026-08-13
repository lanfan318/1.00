package com.hollysys.ppa.module.agent.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.common.BusinessException;
import com.hollysys.ppa.infra.AgentClient;
import com.hollysys.ppa.module.agent.dto.ChatRequestDTO;
import com.hollysys.ppa.module.agent.entity.ChatMessage;
import com.hollysys.ppa.module.agent.entity.ChatSession;
import com.hollysys.ppa.module.agent.mapper.ChatMessageMapper;
import com.hollysys.ppa.module.agent.mapper.ChatSessionMapper;
import com.hollysys.ppa.module.agent.service.AgentService;
import com.hollysys.ppa.module.agent.vo.ChatReplyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Agent 会话 Service 实现（含 AI 对话代理中转）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final AgentClient agentClient;

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
    @Transactional(rollbackFor = Exception.class)
    public ChatSession createSession(String title, String createdBy) {
        ChatSession session = new ChatSession();
        session.setTitle(title);
        session.setStatus("active");
        session.setCreatedBy(createdBy);
        sessionMapper.insert(session);
        log.info("创建 Agent 会话: id={}, title={}", session.getId(), title);
        return session;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatReplyVO chat(ChatRequestDTO dto) {
        long start = System.currentTimeMillis();

        // 校验会话存在
        ChatSession session = sessionMapper.selectById(dto.getSessionId());
        if (session == null) {
            throw new BusinessException("会话不存在: id=" + dto.getSessionId());
        }

        // 1. 落库用户消息
        Integer maxSeq = messageMapper.selectList(
                        new LambdaQueryWrapper<ChatMessage>()
                                .eq(ChatMessage::getSessionId, dto.getSessionId())
                                .orderByDesc(ChatMessage::getSeq)
                                .last("LIMIT 1"))
                .stream().findFirst().map(ChatMessage::getSeq).orElse(0);

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(dto.getSessionId());
        userMsg.setRole("user");
        userMsg.setContent(dto.getMessage());
        userMsg.setSeq(maxSeq + 1);
        messageMapper.insert(userMsg);
        log.info("用户消息落库: sessionId={}, msgId={}", dto.getSessionId(), userMsg.getId());

        // 2. 转发 Agent 服务
        String rawResponse;
        try {
            rawResponse = agentClient.chat(
                    String.valueOf(dto.getSessionId()),
                    dto.getMessage(),
                    dto.getParams());
        } catch (Exception e) {
            log.error("Agent 服务调用失败: sessionId={}", dto.getSessionId(), e);
            // 服务不可用时降级：用本地规则引擎兜底
            rawResponse = JSON.toJSONString(JSONObject.of(
                    "code", 0,
                    "data", JSONObject.of(
                            "content", fallbackAnswer(dto.getMessage()),
                            "source", "local-fallback")));
        }

        // 3. 解析 AI 回复并落库
        String content = extractContent(rawResponse);

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSessionId(dto.getSessionId());
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(content);
        assistantMsg.setRawData(rawResponse);
        assistantMsg.setSeq(maxSeq + 2);
        messageMapper.insert(assistantMsg);

        long cost = System.currentTimeMillis() - start;
        log.info("AI 回复落库: sessionId={}, msgId={}, cost={}ms",
                dto.getSessionId(), assistantMsg.getId(), cost);

        // 4. 返回
        ChatReplyVO vo = new ChatReplyVO();
        vo.setSessionId(dto.getSessionId());
        vo.setUserMessageId(userMsg.getId());
        vo.setAssistantMessageId(assistantMsg.getId());
        vo.setContent(content);
        vo.setRawData(rawResponse);
        vo.setCostMs(cost);
        return vo;
    }

    // ─── 工具方法 ───

    private String extractContent(String rawResponse) {
        try {
            JSONObject json = JSON.parseObject(rawResponse);
            if (json == null) return rawResponse;
            JSONObject data = json.getJSONObject("data");
            if (data != null && data.getString("content") != null) {
                return data.getString("content");
            }
            if (json.getString("content") != null) {
                return json.getString("content");
            }
            if (json.getString("answer") != null) {
                return json.getString("answer");
            }
            return rawResponse;
        } catch (Exception e) {
            return rawResponse;
        }
    }

    /** Agent 服务不可用时的本地兜底回答 */
    private String fallbackAnswer(String question) {
        if (question.contains("报警") || question.contains("告警")) {
            return "当前报警相关信息请查看实时报警页面。我是本地兜底回答，AI 服务暂不可用。";
        }
        if (question.contains("健康") || question.contains("评分")) {
            return "设备健康评分请查看设备管理页面。我是本地兜底回答，AI 服务暂不可用。";
        }
        if (question.contains("曲线") || question.contains("趋势")) {
            return "工况曲线请查看曲线分析页面。我是本地兜底回答，AI 服务暂不可用。";
        }
        return "我是本地兜底回答，AI 服务暂不可用，请稍后重试或联系管理员。";
    }
}
