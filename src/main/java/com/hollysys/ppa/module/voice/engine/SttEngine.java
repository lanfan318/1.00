package com.hollysys.ppa.module.voice.engine;

import java.util.Map;

/**
 * 语音转文字引擎接口（可插拔）
 * 默认 mock 实现，可替换为阿里云 NLS / 讯飞 / 百度等
 */
public interface SttEngine {

    /** 引擎名称 */
    String getName();

    /** 语音转文字 */
    SttResult recognize(byte[] audioData, String format);

    /** 引擎是否可用 */
    boolean isAvailable();

    /** 识别结果 */
    record SttResult(String text, double confidence, long costMs) {}
}
