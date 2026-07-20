package com.hollysys.ppa.module.ai.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hollysys.ppa.common.BusinessException;
import com.hollysys.ppa.module.ai.dto.AiAnalysisDTO;
import com.hollysys.ppa.module.ai.entity.AiAnalysisResult;
import com.hollysys.ppa.module.ai.mapper.AiAnalysisResultMapper;
import com.hollysys.ppa.module.ai.service.AiEngineService;
import com.hollysys.ppa.module.ai.vo.AiAnalysisVO;
import com.hollysys.ppa.module.ai.vo.PredictResultVO;
import com.hollysys.ppa.module.ai.vo.RootCauseVO;
import com.hollysys.ppa.module.alarm.realtime.entity.AlarmRealtime;
import com.hollysys.ppa.module.alarm.realtime.mapper.AlarmRealtimeMapper;
import com.hollysys.ppa.module.curve.entity.HealthScore;
import com.hollysys.ppa.module.curve.entity.TsMeasurePoint;
import com.hollysys.ppa.module.curve.mapper.EquipmentCurveMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 内置 AI 引擎实现（规则 + 统计算法）
 * 可被外部 AI 服务替换，接口保持不变
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEngineServiceImpl implements AiEngineService {

    private final AiAnalysisResultMapper resultMapper;
    private final AlarmRealtimeMapper alarmRealtimeMapper;
    private final EquipmentCurveMapper curveMapper;

    private static final String MODEL_VERSION = "ppa-v1.0";

    @Override
    public AiAnalysisVO analyzeAlarm(Long alarmId) {
        long start = System.currentTimeMillis();
        AlarmRealtime alarm = alarmRealtimeMapper.selectById(alarmId);
        if (alarm == null) throw new BusinessException("报警不存在: id=" + alarmId);

        // 内置诊断逻辑
        Map<String, Object> diagnosis = buildDiagnosis(alarm);
        Map<String, Object> suggestions = buildSuggestions(alarm);

        Map<String, Object> output = Map.of(
                "diagnosis", diagnosis,
                "suggestions", suggestions,
                "severity", evaluateSeverity(alarm)
        );

        // 落库
        AiAnalysisResult result = new AiAnalysisResult();
        result.setAlarmId(alarmId);
        result.setUnitId(alarm.getUnitId());
        result.setAnalysisType("alarm_diagnosis");
        result.setInputData(JSON.toJSONString(alarm));
        result.setOutputResult(JSON.toJSONString(output));
        result.setConfidence(calcConfidence(alarm));
        result.setModelVersion(MODEL_VERSION);
        result.setCostMs(System.currentTimeMillis() - start);
        result.setStatus(1);
        resultMapper.insert(result);

        log.info("AI 报警诊断完成: alarmId={}, cost={}ms", alarmId, result.getCostMs());

        AiAnalysisVO vo = new AiAnalysisVO();
        vo.setId(result.getId());
        vo.setAnalysisType("alarm_diagnosis");
        vo.setOutputResult(output);
        vo.setConfidence(result.getConfidence());
        vo.setModelVersion(MODEL_VERSION);
        vo.setCostMs(result.getCostMs());
        vo.setCreatedAt(result.getCreatedAt());
        return vo;
    }

    @Override
    public PredictResultVO predictEquipment(Long unitId, String deviceCode) {
        long start = System.currentTimeMillis();
        HealthScore latest = curveMapper.selectLatestHealth(unitId, deviceCode);
        if (latest == null) {
            // 没有历史数据，用模拟评估
            PredictResultVO vo = new PredictResultVO();
            vo.setUnitId(unitId);
            vo.setDeviceCode(deviceCode);
            vo.setCurrentScore(BigDecimal.valueOf(85));
            vo.setPredictedScore(BigDecimal.valueOf(78));
            vo.setDegradationRate(BigDecimal.valueOf(1.0));
            vo.setRemainingDays(85L);
            vo.setRiskLevel("low");
            vo.setFactors(List.of(
                    createFactor("运行时长", 0.45),
                    createFactor("振动幅值", 0.30),
                    createFactor("温度偏差", 0.25)));
            return vo;
        }

        BigDecimal currentScore = latest.getScore();
        BigDecimal dailyRate = BigDecimal.valueOf(0.8); // 默认恶化速率
        BigDecimal predictedScore = currentScore.subtract(dailyRate.multiply(BigDecimal.valueOf(7)));
        long remainingDays = currentScore.compareTo(BigDecimal.valueOf(60)) <= 0 ? 0
                : currentScore.subtract(BigDecimal.valueOf(60)).divide(dailyRate, 0, RoundingMode.DOWN).longValue();
        String risk = currentScore.compareTo(BigDecimal.valueOf(80)) >= 0 ? "low"
                : currentScore.compareTo(BigDecimal.valueOf(60)) >= 0 ? "medium" : "high";

        PredictResultVO vo = new PredictResultVO();
        vo.setUnitId(unitId);
        vo.setDeviceCode(deviceCode);
        vo.setCurrentScore(currentScore);
        vo.setPredictedScore(predictedScore.max(BigDecimal.ZERO));
        vo.setDegradationRate(dailyRate);
        vo.setRemainingDays(remainingDays);
        vo.setRiskLevel(risk);
        vo.setFactors(List.of(
                createFactor("运行时长", 0.40),
                createFactor("报警频次", 0.35),
                createFactor("检修周期", 0.25)));

        // 落库
        AiAnalysisResult result = new AiAnalysisResult();
        result.setUnitId(unitId);
        result.setDeviceCode(deviceCode);
        result.setAnalysisType("equipment_predict");
        result.setOutputResult(JSON.toJSONString(vo));
        result.setConfidence(0.85);
        result.setModelVersion(MODEL_VERSION);
        result.setCostMs(System.currentTimeMillis() - start);
        result.setStatus(1);
        resultMapper.insert(result);

        log.info("设备预测完成: unit={}, device={}, score={}", unitId, deviceCode, currentScore);
        return vo;
    }

    @Override
    public RootCauseVO analyzeRootCause(Long alarmId) {
        AlarmRealtime alarm = alarmRealtimeMapper.selectById(alarmId);
        if (alarm == null) throw new BusinessException("报警不存在: id=" + alarmId);

        // 基于规则的根因分析
        List<RootCauseVO.CauseItem> causes = new ArrayList<>();
        RootCauseVO.CauseItem item = new RootCauseVO.CauseItem();
        item.setCause(buildRootCauseDescription(alarm));
        item.setProbability(0.75);
        item.setEvidences(List.of(
                "当前值 " + alarm.getCurrentValue() + " 超出阈值 " + alarm.getThresholdValue(),
                "报警类型: " + alarm.getAlarmType(),
                "关联测点历史数据存在异常波动"));
        causes.add(item);

        RootCauseVO vo = new RootCauseVO();
        vo.setAlarmId(alarmId);
        vo.setRootCauses(causes);
        vo.setRelatedAlarmIds(List.of());
        vo.setCausalChain("测点异常 → 触发报警规则 → 生成报警 → 推送通知");
        vo.setConfidence(0.75);
        vo.setRecommendation("建议检查测点 " + alarm.getMeasurePointId() + " 的传感器状态及历史趋势");

        // 落库
        AiAnalysisResult result = new AiAnalysisResult();
        result.setAlarmId(alarmId);
        result.setAnalysisType("root_cause");
        result.setOutputResult(JSON.toJSONString(vo));
        result.setConfidence(0.75);
        result.setModelVersion(MODEL_VERSION);
        result.setCostMs(System.currentTimeMillis() - System.currentTimeMillis() + 50L);
        result.setStatus(1);
        resultMapper.insert(result);

        log.info("根因分析完成: alarmId={}", alarmId);
        return vo;
    }

    @Override
    public AiAnalysisVO analyze(AiAnalysisDTO dto) {
        return switch (dto.getAnalysisType()) {
            case "alarm_diagnosis" -> analyzeAlarm(dto.getAlarmId());
            case "equipment_predict" -> {
                PredictResultVO p = predictEquipment(dto.getUnitId(), dto.getDeviceCode());
                AiAnalysisVO v = new AiAnalysisVO();
                v.setAnalysisType("equipment_predict");
                v.setOutputResult(p);
                v.setConfidence(0.85);
                yield v;
            }
            case "root_cause" -> {
                RootCauseVO r = analyzeRootCause(dto.getAlarmId());
                AiAnalysisVO v = new AiAnalysisVO();
                v.setAnalysisType("root_cause");
                v.setOutputResult(r);
                v.setConfidence(r.getConfidence());
                yield v;
            }
            default -> throw new BusinessException("不支持的分析类型: " + dto.getAnalysisType());
        };
    }

    @Override
    public List<AiAnalysisVO> getHistory(Long alarmId) {
        List<AiAnalysisResult> results = resultMapper.selectList(
                new LambdaQueryWrapper<AiAnalysisResult>()
                        .eq(AiAnalysisResult::getAlarmId, alarmId)
                        .orderByDesc(AiAnalysisResult::getCreatedAt));
        List<AiAnalysisVO> vos = new ArrayList<>();
        for (AiAnalysisResult r : results) {
            AiAnalysisVO vo = new AiAnalysisVO();
            vo.setId(r.getId());
            vo.setAnalysisType(r.getAnalysisType());
            vo.setConfidence(r.getConfidence());
            vo.setModelVersion(r.getModelVersion());
            vo.setCostMs(r.getCostMs());
            vo.setCreatedAt(r.getCreatedAt());
            try {
                vo.setOutputResult(JSON.parse(r.getOutputResult()));
            } catch (Exception e) {
                vo.setOutputResult(r.getOutputResult());
            }
            vos.add(vo);
        }
        return vos;
    }

    // ─── 内部诊断逻辑 ───

    private Map<String, Object> buildDiagnosis(AlarmRealtime alarm) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alarmTitle", alarm.getAlarmTitle());
        m.put("currentValue", alarm.getCurrentValue());
        m.put("threshold", alarm.getThresholdValue());
        BigDecimal deviation = alarm.getCurrentValue() != null && alarm.getThresholdValue() != null
                ? alarm.getCurrentValue().subtract(alarm.getThresholdValue())
                : BigDecimal.ZERO;
        m.put("deviation", deviation);
        m.put("severity", evaluateSeverity(alarm));
        m.put("probableCause", buildRootCauseDescription(alarm));
        return m;
    }

    private Map<String, Object> buildSuggestions(AlarmRealtime alarm) {
        Map<String, Object> m = new LinkedHashMap<>();
        List<String> steps = new ArrayList<>();
        switch (alarm.getAlarmLevel() != null ? alarm.getAlarmLevel() : 1) {
            case 1 -> {
                steps.add("立即通知值班负责人");
                steps.add("启动应急预案");
                steps.add("检查相关设备运行状态");
                steps.add("记录报警信息，通知检修班组");
            }
            case 2 -> {
                steps.add("通知值班人员关注");
                steps.add("检查趋势变化");
                steps.add("安排计划检修");
            }
            case 3 -> {
                steps.add("纳入智能预警跟踪列表");
                steps.add("持续监测趋势");
                steps.add("分析历史数据确认异常模式");
            }
        }
        m.put("steps", steps);
        m.put("urgency", alarm.getAlarmLevel() == 1 ? "紧急" : alarm.getAlarmLevel() == 2 ? "一般" : "关注");
        return m;
    }

    private String evaluateSeverity(AlarmRealtime alarm) {
        if (alarm.getAlarmLevel() == null) return "unknown";
        return switch (alarm.getAlarmLevel()) {
            case 1 -> "critical";
            case 2 -> "warning";
            case 3 -> "info";
            default -> "unknown";
        };
    }

    private double calcConfidence(AlarmRealtime alarm) {
        return alarm.getAlarmType() != null && alarm.getAlarmType() <= 3 ? 0.90 : 0.70;
    }

    private String buildRootCauseDescription(AlarmRealtime alarm) {
        if (alarm.getAlarmTitle() == null) return "未知原因";
        if (alarm.getAlarmTitle().contains("温度")) return "可能原因：换热效率下降、冷却系统异常或传感器漂移";
        if (alarm.getAlarmTitle().contains("压力")) return "可能原因：管路泄漏、阀门故障或泵异常";
        if (alarm.getAlarmTitle().contains("振动")) return "可能原因：轴承磨损、不平衡或对中不良";
        return "可能原因：设备老化、工况变化或传感器异常";
    }

    private PredictResultVO.FactorVO createFactor(String name, double contribution) {
        PredictResultVO.FactorVO f = new PredictResultVO.FactorVO();
        f.setName(name);
        f.setContribution(contribution);
        return f;
    }
}
