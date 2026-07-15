package com.hollysys.ppa.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Agent 服务 HTTP 客户端
 *
 * @author PPA Team
 */
@Slf4j
@Component
public class AgentClient {

    private final RestTemplate restTemplate;

    @Value("${agent.service.url}")
    private String agentServiceUrl;

    public AgentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 向 Agent 服务发送消息（SSE 流式，本客户端只拿最终结果）
     *
     * @param sessionId 会话ID
     * @param message   用户消息
     * @param params    附加参数
     * @return Agent 响应 JSON
     */
    public String chat(String sessionId, String message, Map<String, Object> params) {
        String url = agentServiceUrl + "/api/agent/chat";
        log.info("Agent 对话请求: sessionId={}", sessionId);

        Map<String, Object> body = Map.of(
                "sessionId", sessionId,
                "message", message,
                "params", params != null ? params : Map.of()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("Agent 对话响应: sessionId={}, status={}", sessionId, response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            log.error("Agent 服务调用失败: sessionId={}", sessionId, e);
            throw new RuntimeException("Agent 服务调用失败", e);
        }
    }
}
