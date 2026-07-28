package com.hollysys.ppa.module.voice.engine.impl;

import com.hollysys.ppa.module.voice.engine.SttEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock STT 引擎（开发测试用）
 * 生产环境替换为真实语音识别服务
 */
@Slf4j
@Component
public class MockSttEngine implements SttEngine {

    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public SttResult recognize(byte[] audioData, String format) {
        long start = System.currentTimeMillis();
        log.info("Mock STT: 收到音频 {} bytes, format={}", audioData.length, format);

        // 模拟识别耗时
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        String mockText;
        if (audioData.length < 100) {
            mockText = "音频过短，无法识别";
        } else if (audioData.length < 5000) {
            mockText = "查询当前报警";
        } else if (audioData.length < 20000) {
            mockText = "查看一号机组快照";
        } else {
            mockText = "分析设备健康状态";
        }

        long cost = System.currentTimeMillis() - start;
        log.info("Mock STT 结果: \"{}\", confidence=0.92, cost={}ms", mockText, cost);
        return new SttResult(mockText, 0.92, cost);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
