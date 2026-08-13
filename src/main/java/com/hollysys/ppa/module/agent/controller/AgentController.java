package com.hollysys.ppa.module.agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.agent.dto.ChatRequestDTO;
import com.hollysys.ppa.module.agent.entity.ChatMessage;
import com.hollysys.ppa.module.agent.entity.ChatSession;
import com.hollysys.ppa.module.agent.service.AgentService;
import com.hollysys.ppa.module.agent.vo.ChatReplyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 会话 Controller（含 AI 对话代理中转）
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "Agent 会话", description = "AI 对话代理中转 / 会话管理 / 消息查询")
public class AgentController {

    private final AgentService agentService;

    @GetMapping("/sessions")
    @Operation(summary = "查询会话列表")
    public R<Page<ChatSession>> listSessions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return R.ok(agentService.listSessions(page, size));
    }

    @GetMapping("/sessions/{id}/messages")
    @Operation(summary = "查询会话消息列表")
    public R<List<ChatMessage>> getMessages(@PathVariable Long id) {
        return R.ok(agentService.getMessages(id));
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建新会话")
    public R<ChatSession> createSession(@RequestBody java.util.Map<String, String> body) {
        String title = body.getOrDefault("title", "新对话");
        String createdBy = body.getOrDefault("createdBy", "system");
        return R.ok(agentService.createSession(title, createdBy));
    }

    @PostMapping("/chat")
    @Operation(summary = "AI 对话代理中转",
            description = "前端发消息 → 落库用户消息 → 转发 Agent 服务 → 落库 AI 回复 → 返回。Agent 服务不可用时自动降级为本地兜底回答")
    public R<ChatReplyVO> chat(@Valid @RequestBody ChatRequestDTO dto) {
        log.info("AI 对话请求: sessionId={}, msgLen={}", dto.getSessionId(), dto.getMessage().length());
        return R.ok(agentService.chat(dto));
    }
}
