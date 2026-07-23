package com.hollysys.ppa.module.realtime.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hollysys.ppa.module.alarm.realtime.entity.AlarmRealtime;
import com.hollysys.ppa.module.alarm.realtime.mapper.AlarmRealtimeMapper;
import com.hollysys.ppa.module.equipment.entity.DimMeasurePoint;
import com.hollysys.ppa.module.equipment.entity.DimUnit;
import com.hollysys.ppa.module.equipment.mapper.DimMeasurePointMapper;
import com.hollysys.ppa.module.equipment.mapper.DimUnitMapper;
import com.hollysys.ppa.module.realtime.service.RealtimeDataService;
import com.hollysys.ppa.module.realtime.vo.RealtimeCurveVO;
import com.hollysys.ppa.module.realtime.vo.RealtimePointVO;
import com.hollysys.ppa.module.realtime.vo.SnapshotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 实时数据 Service 实现
 * 查询 ts_measure_point 时序表的最新数据，结合 AI 算法做异常标记
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeDataServiceImpl implements RealtimeDataService {

    private final JdbcTemplate jdbcTemplate;
    private final DimMeasurePointMapper measurePointMapper;
    private final DimUnitMapper dimUnitMapper;
    private final AlarmRealtimeMapper alarmRealtimeMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<RealtimePointVO> getLatestValues(Long unitId, List<Long> pointIds) {
        if (pointIds == null || pointIds.isEmpty()) {
            // 没有指定测点则取该机组全部测点
            List<DimMeasurePoint> all = measurePointMapper.selectList(
                    new LambdaQueryWrapper<DimMeasurePoint>()
                            .eq(DimMeasurePoint::getUnitId, unitId)
                            .last("LIMIT 50"));
            pointIds = all.stream().map(DimMeasurePoint::getId).toList();
        }

        List<RealtimePointVO> result = new ArrayList<>();
        for (Long pid : pointIds) {
            RealtimePointVO vo = buildPointVO(unitId, pid);
            if (vo != null) result.add(vo);
        }
        return result;
    }

    @Override
    public RealtimeCurveVO getRealtimeCurve(Long unitId, String deviceCode, String curveType, Integer seconds) {
        int secs = seconds != null ? Math.min(seconds, 3600) : 300; // 默认 5 分钟，最大 1 小时
        LocalDateTime start = LocalDateTime.now().minusSeconds(secs);

        // 从 ts_measure_point 查最近 N 秒的数据
        String sql = "SELECT mp.point_code, mp.point_name, ts.value, ts.ts " +
                     "FROM ts_measure_point ts " +
                     "JOIN dim_measure_point mp ON ts.measure_point_id = mp.id " +
                     "WHERE ts.unit_id = ? AND mp.system_id = " +
                     "(SELECT id FROM dim_system WHERE system_code = ? LIMIT 1) " +
                     "AND ts.ts >= ? ORDER BY ts.ts ASC";

        // 简化查询：按时间和机组取数据
        String simpleSql = "SELECT ts.ts, ts.value FROM ts_measure_point ts " +
                           "WHERE ts.unit_id = ? AND ts.ts >= ? ORDER BY ts.ts ASC LIMIT 1000";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(simpleSql, unitId, start);

        List<RealtimeCurveVO.CurvePoint> points = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            RealtimeCurveVO.CurvePoint p = new RealtimeCurveVO.CurvePoint();
            p.setTimestamp(row.get("ts").toString());
            BigDecimal val = (BigDecimal) row.get("value");
            p.setValue(val);
            points.add(p);
            values.add(val);
        }

        // 统计
        RealtimeCurveVO.CurveStats stats = new RealtimeCurveVO.CurveStats();
        if (!values.isEmpty()) {
            stats.setMin(values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            stats.setMax(values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            stats.setAvg(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP));

            // 趋势判断：最后 5 个点的变化方向
            if (values.size() >= 5) {
                BigDecimal first5Avg = values.subList(0, 5).stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP);
                int lastIdx = values.size();
                BigDecimal last5Avg = values.subList(lastIdx - 5, lastIdx).stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP);
                BigDecimal diff = last5Avg.subtract(first5Avg);
                if (diff.compareTo(BigDecimal.valueOf(0.5)) > 0) stats.setTrend("up");
                else if (diff.compareTo(BigDecimal.valueOf(-0.5)) < 0) stats.setTrend("down");
                else stats.setTrend("stable");
            }
        }

        // 异常检测（3-sigma 简化版）
        RealtimeCurveVO.AnomalyInfo anomaly = new RealtimeCurveVO.AnomalyInfo();
        if (values.size() >= 10) {
            BigDecimal mean = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
            BigDecimal sumSq = BigDecimal.ZERO;
            for (BigDecimal v : values) {
                sumSq = sumSq.add(v.subtract(mean).pow(2));
            }
            BigDecimal std = BigDecimal.valueOf(Math.sqrt(
                    sumSq.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP).doubleValue()));
            BigDecimal upper = mean.add(std.multiply(BigDecimal.valueOf(3)));

            long anomalyCount = values.stream().filter(v -> v.compareTo(upper) > 0).count();
            anomaly.setHasAnomaly(anomalyCount > 0);
            anomaly.setAnomalyCount((int) anomalyCount);
            anomaly.setDescription(anomalyCount > 0 ? "检测到 " + anomalyCount + " 个异常数据点" : "数据正常");
        } else {
            anomaly.setHasAnomaly(false);
            anomaly.setDescription("数据点不足无法检测");
        }

        RealtimeCurveVO vo = new RealtimeCurveVO();
        vo.setUnitId(unitId);
        vo.setDeviceCode(deviceCode);
        vo.setCurveType(curveType);
        vo.setPoints(points);
        vo.setStats(stats);
        vo.setAnomalyInfo(anomaly);

        return vo;
    }

    @Override
    public SnapshotVO getSnapshot(Long unitId) {
        DimUnit unit = dimUnitMapper.selectById(unitId);

        // 获取该机组所有测点
        List<DimMeasurePoint> allPoints = measurePointMapper.selectList(
                new LambdaQueryWrapper<DimMeasurePoint>()
                        .eq(DimMeasurePoint::getUnitId, unitId)
                        .last("LIMIT 100"));

        List<RealtimePointVO> pointVOs = new ArrayList<>();
        int online = 0, anomaly = 0;
        double totalHealth = 0;

        for (DimMeasurePoint mp : allPoints) {
            RealtimePointVO vo = buildPointVO(unitId, mp.getId());
            if (vo != null) {
                pointVOs.add(vo);
                online++;
                if (Boolean.TRUE.equals(vo.getAnomaly())) anomaly++;
                if (vo.getHealthScore() != null) totalHealth += vo.getHealthScore().doubleValue();
            }
        }

        // 查询当前报警数
        Long alarmCount = alarmRealtimeMapper.selectCount(
                new LambdaQueryWrapper<AlarmRealtime>()
                        .eq(AlarmRealtime::getUnitId, unitId)
                        .eq(AlarmRealtime::getSuppressed, 0));

        SnapshotVO vo = new SnapshotVO();
        vo.setUnitId(unitId);
        vo.setUnitName(unit != null ? unit.getUnitName() : "未知");
        vo.setTotalPoints(allPoints.size());
        vo.setOnlinePoints(online);
        vo.setAnomalyPoints(anomaly);
        vo.setAlarmCount(alarmCount != null ? alarmCount.intValue() : 0);
        vo.setOverallHealth(online > 0 ? Math.round(totalHealth / online * 10.0) / 10.0 : 100.0);
        vo.setPoints(pointVOs);

        log.info("机组快照: unit={}, points={}, online={}, anomaly={}, alarms={}",
                unitId, allPoints.size(), online, anomaly, alarmCount);

        return vo;
    }

    // ─── 工具方法 ───

    private RealtimePointVO buildPointVO(Long unitId, Long pointId) {
        DimMeasurePoint mp = measurePointMapper.selectById(pointId);
        if (mp == null) return null;

        // 查最新一条时序数据
        String sql = "SELECT ts, value, quality_flag FROM ts_measure_point " +
                     "WHERE measure_point_id = ? AND unit_id = ? " +
                     "ORDER BY ts DESC LIMIT 2";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, pointId, unitId);

        if (rows.isEmpty()) return null;

        Map<String, Object> latest = rows.get(0);
        BigDecimal currentValue = (BigDecimal) latest.get("value");

        // 计算趋势（和前一个值比较）
        String trend = "stable";
        BigDecimal delta = BigDecimal.ZERO;
        if (rows.size() >= 2) {
            BigDecimal prevValue = (BigDecimal) rows.get(1).get("value");
            delta = currentValue.subtract(prevValue);
            if (delta.compareTo(BigDecimal.valueOf(0.01)) > 0) trend = "up";
            else if (delta.compareTo(BigDecimal.valueOf(-0.01)) < 0) trend = "down";
        }

        // 简单异常检测：超出量程
        boolean isAnomaly = false;
        if (mp.getRangeLow() != null && currentValue.compareTo(BigDecimal.valueOf(mp.getRangeLow())) < 0) {
            isAnomaly = true;
        }
        if (mp.getRangeHigh() != null && currentValue.compareTo(BigDecimal.valueOf(mp.getRangeHigh())) > 0) {
            isAnomaly = true;
        }

        // 计算健康评分（简化：在量程范围内则为高分）
        BigDecimal health = BigDecimal.valueOf(100);
        if (mp.getRangeHigh() != null && mp.getRangeLow() != null) {
            double range = mp.getRangeHigh() - mp.getRangeLow();
            double mid = (mp.getRangeHigh() + mp.getRangeLow()) / 2;
            double deviation = Math.abs(currentValue.doubleValue() - mid);
            health = BigDecimal.valueOf(Math.max(0, 100 - (deviation / range * 50)));
        }

        // 查是否有未抑制报警
        Long alarmCount = alarmRealtimeMapper.selectCount(
                new LambdaQueryWrapper<AlarmRealtime>()
                        .eq(AlarmRealtime::getMeasurePointId, pointId)
                        .eq(AlarmRealtime::getSuppressed, 0));

        RealtimePointVO vo = new RealtimePointVO();
        vo.setPointId(pointId);
        vo.setPointCode(mp.getPointCode());
        vo.setPointName(mp.getPointName());
        vo.setCurrentValue(currentValue);
        vo.setUnit(mp.getUnit());
        vo.setSampleTime(latest.get("ts").toString());
        vo.setQualityFlag((Integer) latest.get("quality_flag"));
        vo.setTrend(trend);
        vo.setDelta(delta);
        vo.setAnomaly(isAnomaly);
        vo.setAlarmLevel(alarmCount > 0 ? 1 : null);
        vo.setHealthScore(health.setScale(1, RoundingMode.HALF_UP));

        return vo;
    }
}
