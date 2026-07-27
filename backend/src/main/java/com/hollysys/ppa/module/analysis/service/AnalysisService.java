package com.hollysys.ppa.module.analysis.service;

import com.hollysys.ppa.module.analysis.vo.*;

import java.util.List;

/**
 * 数据分析算法 Service
 */
public interface AnalysisService {

    /** 异常检测：3-sigma + 移动平均 */
    AnomalyResultVO detectAnomaly(Long unitId, Long measurePointId, String range);

    /** 趋势预测：线性回归 */
    TrendResultVO predictTrend(Long unitId, Long measurePointId, Integer forecastHours);

    /** 健康评分计算 */
    HealthCalcResultVO calculateHealth(Long unitId, String deviceCode);

    /** 报警关联分析 */
    CorrelationResultVO analyzeCorrelation(Long alarmId, Integer windowMinutes);

    /** 实时数据统计 */
    List<RealTimeStatsVO> realTimeStats(Long unitId);
}
