package com.hollysys.ppa.module.analysis.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.hollysys.ppa.common.BusinessException;
import com.hollysys.ppa.module.alarm.realtime.entity.AlarmHistory;
import com.hollysys.ppa.module.alarm.realtime.mapper.AlarmRealtimeMapper;
import com.hollysys.ppa.module.analysis.service.AnalysisService;
import com.hollysys.ppa.module.analysis.vo.*;
import com.hollysys.ppa.module.curve.entity.TsMeasurePoint;
import com.hollysys.ppa.module.equipment.entity.DimMeasurePoint;
import com.hollysys.ppa.module.equipment.mapper.DimMeasurePointMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据分析算法实现
 * 算法：3-sigma 异常检测 / 线性回归趋势预测 / 加权健康评分 / 时间窗口关联分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final JdbcTemplate jdbcTemplate;
    private final DimMeasurePointMapper measurePointMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public AnomalyResultVO detectAnomaly(Long unitId, Long measurePointId, String range) {
        List<TsMeasurePoint> data = queryMeasurePoints(measurePointId, unitId, resolveMinutes(range));

        if (data.size() < 10) {
            throw new BusinessException("数据点不足，至少需要 10 个点");
        }

        // 计算均值和标准差
        BigDecimal mean = calcMean(data);
        BigDecimal std = calcStd(data, mean);
        BigDecimal threeSigma = std.multiply(BigDecimal.valueOf(3));
        BigDecimal upper = mean.add(threeSigma);
        BigDecimal lower = mean.subtract(threeSigma);

        // 标记异常点
        List<AnomalyResultVO.AnomalyPoint> anomalies = new ArrayList<>();
        for (TsMeasurePoint p : data) {
            if (p.getValue().compareTo(upper) > 0 || p.getValue().compareTo(lower) < 0) {
                AnomalyResultVO.AnomalyPoint ap = new AnomalyResultVO.AnomalyPoint();
                ap.setTimestamp(p.getTs().format(DT_FMT));
                ap.setValue(p.getValue());
                ap.setUpperBound(upper);
                ap.setLowerBound(lower);
                ap.setDeviationSigma(p.getValue().subtract(mean).abs().divide(std.max(BigDecimal.valueOf(0.001)), 2, RoundingMode.HALF_UP).doubleValue());
                anomalies.add(ap);
            }
        }

        AnomalyResultVO vo = new AnomalyResultVO();
        vo.setMeasurePointId(measurePointId);
        vo.setAnomalyCount(anomalies.size());
        vo.setAnomalyRate(anomalies.size() * 100.0 / data.size());
        vo.setPoints(anomalies);

        log.info("异常检测完成: point={}, anomalies={}/{}", measurePointId, anomalies.size(), data.size());
        return vo;
    }

    @Override
    public TrendResultVO predictTrend(Long unitId, Long measurePointId, Integer forecastHours) {
        List<TsMeasurePoint> data = queryMeasurePoints(measurePointId, unitId, 1440); // 最近 24 小时

        if (data.size() < 5) {
            throw new BusinessException("数据点不足");
        }

        // 简单线性回归：y = slope * x + intercept
        int n = data.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = data.get(i).getValue().doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            sumY2 += y * y;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) denominator = 1;

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        double rNumerator = n * sumXY - sumX * sumY;
        double rDenom = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
        double rSquared = rDenom > 0 ? Math.pow(rNumerator / rDenom, 2) : 0;

        // 预测点
        BigDecimal slopeBd = BigDecimal.valueOf(slope);
        BigDecimal interceptBd = BigDecimal.valueOf(intercept);
        List<TrendResultVO.PredictPoint> predictions = new ArrayList<>();
        LocalDateTime lastTs = data.get(n - 1).getTs();

        for (int i = 1; i <= forecastHours; i++) {
            TrendResultVO.PredictPoint pp = new TrendResultVO.PredictPoint();
            pp.setTimestamp(lastTs.plusMinutes(i * 60 / (n > 24 ? n / 24 : 1)).format(DT_FMT));
            pp.setValue(interceptBd.add(slopeBd.multiply(BigDecimal.valueOf(n + i))));
            predictions.add(pp);
        }

        TrendResultVO vo = new TrendResultVO();
        vo.setMeasurePointId(measurePointId);
        vo.setSlope(slopeBd);
        vo.setIntercept(interceptBd);
        vo.setRSquared(Math.round(rSquared * 100.0) / 100.0);
        vo.setDirection(slope > 0.01 ? "up" : slope < -0.01 ? "down" : "stable");
        vo.setPredictions(predictions);

        log.info("趋势预测完成: point={}, slope={}, direction={}", measurePointId, slopeBd, vo.getDirection());
        return vo;
    }

    @Override
    public HealthCalcResultVO calculateHealth(Long unitId, String deviceCode) {
        // 加权评分算法：多个维度 → 综合评分
        Map<String, BigDecimal> dimScores = new LinkedHashMap<>();
        Map<String, Double> weights = new LinkedHashMap<>();

        // 模拟从时序数据计算各维度得分
        dimScores.put("运行稳定性", BigDecimal.valueOf(88));
        weights.put("运行稳定性", 0.3);
        dimScores.put("报警频率", BigDecimal.valueOf(92));
        weights.put("报警频率", 0.2);
        dimScores.put("参数偏差", BigDecimal.valueOf(75));
        weights.put("参数偏差", 0.2);
        dimScores.put("检修周期", BigDecimal.valueOf(80));
        weights.put("检修周期", 0.15);
        dimScores.put("环境因素", BigDecimal.valueOf(90));
        weights.put("环境因素", 0.15);

        // 加权求和
        BigDecimal total = BigDecimal.ZERO;
        for (String key : dimScores.keySet()) {
            total = total.add(dimScores.get(key).multiply(BigDecimal.valueOf(weights.get(key))));
        }

        String grade;
        if (total.compareTo(BigDecimal.valueOf(90)) >= 0) grade = "excellent";
        else if (total.compareTo(BigDecimal.valueOf(75)) >= 0) grade = "good";
        else if (total.compareTo(BigDecimal.valueOf(60)) >= 0) grade = "fair";
        else if (total.compareTo(BigDecimal.valueOf(40)) >= 0) grade = "poor";
        else grade = "critical";

        String suggestion;
        switch (grade) {
            case "excellent" -> suggestion = "设备状态良好，按计划维护即可";
            case "good" -> suggestion = "设备运行正常，建议关注参数偏差趋势";
            case "fair" -> suggestion = "建议缩短巡检周期，排查潜在隐患";
            case "poor" -> suggestion = "建议尽快安排检修，存在较高故障风险";
            default -> suggestion = "设备状态危急，建议立即停机检修";
        }

        HealthCalcResultVO vo = new HealthCalcResultVO();
        vo.setTotalScore(total.setScale(1, RoundingMode.HALF_UP));
        vo.setGrade(grade);
        vo.setDimensionScores(dimScores);
        vo.setWeights(weights);
        vo.setSuggestion(suggestion);

        log.info("健康评分计算: unit={}, device={}, score={}, grade={}", unitId, deviceCode, vo.getTotalScore(), grade);
        return vo;
    }

    @Override
    public CorrelationResultVO analyzeCorrelation(Long alarmId, Integer windowMinutes) {
        // 时间窗口内查找关联报警
        CorrelationResultVO vo = new CorrelationResultVO();
        vo.setSourceAlarmId(alarmId);
        vo.setWindowMinutes(windowMinutes);
        vo.setCorrelatedAlarms(List.of());

        // TODO: 从 t_alarm_history 中查找同窗口内发生的其他报警
        log.info("报警关联分析: alarmId={}, window={}min", alarmId, windowMinutes);
        return vo;
    }

    @Override
    public List<RealTimeStatsVO> realTimeStats(Long unitId) {
        // 查询该机组所有测点的实时统计
        List<DimMeasurePoint> points = measurePointMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DimMeasurePoint>()
                        .eq(DimMeasurePoint::getUnitId, unitId)
                        .last("LIMIT 20"));

        List<RealTimeStatsVO> stats = new ArrayList<>();
        LocalDateTime start = LocalDateTime.now().minusMinutes(30);

        for (DimMeasurePoint mp : points) {
            List<TsMeasurePoint> data = queryMeasurePoints(mp.getId(), unitId, 30);
            if (data.isEmpty()) continue;

            BigDecimal mean = calcMean(data);
            BigDecimal std = calcStd(data, mean);
            BigDecimal max = data.stream().map(TsMeasurePoint::getValue).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal min = data.stream().map(TsMeasurePoint::getValue).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal current = data.get(data.size() - 1).getValue();
            BigDecimal change = mean.compareTo(BigDecimal.ZERO) != 0
                    ? current.subtract(mean).divide(mean, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            stats.add(new RealTimeStatsVO(
                    mp.getId(), mp.getPointName(),
                    current, mean, max, min, std,
                    (long) data.size(), change));
        }

        log.info("实时统计: unit={}, points={}", unitId, stats.size());
        return stats;
    }

    // ─── 工具方法 ───

    private List<TsMeasurePoint> queryMeasurePoints(Long measurePointId, Long unitId, int minutesBack) {
        LocalDateTime start = LocalDateTime.now().minusMinutes(minutesBack);
        String sql = "SELECT ts, measure_point_id, unit_id, value, quality_flag " +
                     "FROM ts_measure_point WHERE measure_point_id = ? AND unit_id = ? " +
                     "AND ts >= ? ORDER BY ts ASC";
        return jdbcTemplate.query(sql,
                (rs, i) -> {
                    TsMeasurePoint p = new TsMeasurePoint();
                    p.setTs(rs.getTimestamp("ts").toLocalDateTime());
                    p.setMeasurePointId(rs.getLong("measure_point_id"));
                    p.setUnitId(rs.getLong("unit_id"));
                    p.setValue(rs.getBigDecimal("value"));
                    p.setQualityFlag(rs.getInt("quality_flag"));
                    return p;
                },
                measurePointId, unitId, start);
    }

    private int resolveMinutes(String range) {
        return switch (range) {
            case "1h" -> 60;
            case "6h" -> 360;
            case "24h" -> 1440;
            case "7d" -> 10080;
            default -> 1440;
        };
    }

    private BigDecimal calcMean(List<TsMeasurePoint> data) {
        BigDecimal sum = data.stream().map(TsMeasurePoint::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(data.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal calcStd(List<TsMeasurePoint> data, BigDecimal mean) {
        BigDecimal sumSq = BigDecimal.ZERO;
        for (TsMeasurePoint p : data) {
            BigDecimal diff = p.getValue().subtract(mean);
            sumSq = sumSq.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSq.divide(BigDecimal.valueOf(data.size()), 6, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }
}
