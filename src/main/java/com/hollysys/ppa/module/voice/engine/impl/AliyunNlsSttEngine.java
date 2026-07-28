package com.hollysys.ppa.module.voice.engine.impl;

import com.hollysys.ppa.module.voice.engine.SttEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 阿里云 NLS 语音识别引擎（需配置 AccessKey 启用）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "aliyun.nls", name = "enabled", havingValue = "true")
public class AliyunNlsSttEngine implements SttEngine {

    @Value("${aliyun.nls.app-key:}")
    private String appKey;

    @Value("${aliyun.nls.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.nls.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.nls.endpoint:https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1/asr}")
    private String endpoint;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String getName() {
        return "aliyun_nls";
    }

    @Override
    public SttResult recognize(byte[] audioData, String format) {
        long start = System.currentTimeMillis();
        log.info("阿里云 NLS 识别: appKey={}, size={}bytes", appKey, audioData.length);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("X-NLS-Token", obtainToken())
                    .header("Content-Type", "application/octet-stream")
                    .header("X-NLS-AppKey", appKey)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(audioData))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long cost = System.currentTimeMillis() - start;

            String text = extractText(response.body());
            log.info("NLS 结果: \"{}\", cost={}ms", text, cost);
            return new SttResult(text, 0.95, cost);
        } catch (Exception e) {
            log.error("阿里云 NLS 识别失败", e);
            return new SttResult("", 0, System.currentTimeMillis() - start);
        }
    }

    @Override
    public boolean isAvailable() {
        return appKey != null && !appKey.isBlank()
                && accessKeyId != null && !accessKeyId.isBlank();
    }

    private String obtainToken() {
        // TODO: 通过 AccessKey 获取 NLS Token
        return "mock-token";
    }

    private String extractText(String responseBody) {
        // TODO: 解析 NLS 响应 JSON
        return responseBody;
    }
}
