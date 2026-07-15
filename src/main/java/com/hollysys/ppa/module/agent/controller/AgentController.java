package com.hollysys.ppa.module.agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.agent.entity.ChatMessage;
import com.hollysys.ppa.module.agent.entity.ChatSession;
import com.hollysys.ppa.module.agent.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent 会话 Controller
 *
 * @author PPA Team
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "Agent 会话", description = "Agent 会话管理 / 消息查询")
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
    public R<ChatSession> createSession(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "新对话");
        String createdBy = body.getOrDefault("createdBy", "system");
        return R.ok(agentService.createSession(title, createdBy));
    }
}
