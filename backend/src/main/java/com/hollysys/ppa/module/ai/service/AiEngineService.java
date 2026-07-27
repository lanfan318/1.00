package com.hollysys.ppa.module.ai.service;

import com.hollysys.ppa.module.ai.dto.AiAnalysisDTO;
import com.hollysys.ppa.module.ai.vo.AiAnalysisVO;
import com.hollysys.ppa.module.ai.vo.PredictResultVO;
import com.hollysys.ppa.module.ai.vo.RootCauseVO;

import java.util.List;

/**
 * 内置 AI 引擎 Service —— 核心推理能力
 */
public interface AiEngineService {

    /**
     * AI 分析报警（诊断 + 建议）
     */
    AiAnalysisVO analyzeAlarm(Long alarmId);

    /**
     * 设备状态预测
     */
    PredictResultVO predictEquipment(Long unitId, String deviceCode);

    /**
     * 报警根因分析
     */
    RootCauseVO analyzeRootCause(Long alarmId);

    /**
     * 通用 AI 分析入口
     */
    AiAnalysisVO analyze(AiAnalysisDTO dto);

    /**
     * 查询历史分析结果
     */
    List<AiAnalysisVO> getHistory(Long alarmId);
}
