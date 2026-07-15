package com.hollysys.ppa.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 知识图谱 (KG) 服务 HTTP 客户端
 *
 * @author PPA Team
 */
@Slf4j
@Component
public class KgClient {

    private final RestTemplate restTemplate;

    @Value("${kg.service.url}")
    private String kgServiceUrl;

    public KgClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 向 KG 服务发起推理请求（转发 + 落库由调用方完成）
     *
     * @param alarmId 报警ID
     * @param params 请求参数
     * @return KG 服务响应 JSON
     */
    public String reasoning(Long alarmId, Map<String, Object> params) {
        String url = kgServiceUrl + "/api/kg/reasoning/" + alarmId;
        log.info("KG 推理请求: url={}, alarmId={}", url, alarmId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("KG 推理响应: alarmId={}, status={}", alarmId, response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            log.error("KG 服务调用失败: alarmId={}, url={}", alarmId, url, e);
            throw new RuntimeException("KG 服务调用失败", e);
        }
    }

    /**
     * 获取操作指导
     */
    public String getOperationGuide(Long alarmId) {
        String url = kgServiceUrl + "/api/kg/guide/" + alarmId;
        log.info("KG 操作指导请求: url={}", url);
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("KG 操作指导获取失败: alarmId={}", alarmId, e);
            throw new RuntimeException("KG 操作指导获取失败", e);
        }
    }
}
