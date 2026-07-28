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
import java.util.stream.Collectors;

/**
 * 实时数据 Service 实现（批量查询优化，消除 N+1）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeDataServiceImpl implements RealtimeDataService {

    private final JdbcTemplate jdbcTemplate;
    private final DimMeasurePointMapper measurePointMapper;
    private final DimUnitMapper dimUnitMapper;
    private final AlarmRealtimeMapper alarmRealtimeMapper;

    private static final int MAX_POINTS = 50;

    @Override
    public List<RealtimePointVO> getLatestValues(Long unitId, List<Long> pointIds) {
        // 1. 批量加载测点信息
        if (pointIds == null || pointIds.isEmpty()) {
            List<DimMeasurePoint> all = measurePointMapper.selectList(
                    new LambdaQueryWrapper<DimMeasurePoint>()
                            .eq(DimMeasurePoint::getUnitId, unitId)
                            .last("LIMIT " + MAX_POINTS));
            pointIds = all.stream().map(DimMeasurePoint::getId).collect(Collectors.toList());
        }
        if (pointIds.isEmpty()) return List.of();

        Map<Long, DimMeasurePoint> pointMap = measurePointMapper.selectBatchIds(pointIds).stream()
                .collect(Collectors.toMap(DimMeasurePoint::getId, p -> p));

        // 2. 批量查询时序数据（一条 SQL 查所有测点最新值）
        List<Map<String, Object>> tsRows = batchQueryLastTwoValues(unitId, pointIds);

        // 3. 批量查询报警状态
        Set<Long> alarmingPointIds = alarmRealtimeMapper.selectList(
                new LambdaQueryWrapper<AlarmRealtime>()
                        .in(AlarmRealtime::getMeasurePointId, pointIds)
                        .eq(AlarmRealtime::getSuppressed, 0)
                        .select(AlarmRealtime::getMeasurePointId))
                .stream().map(AlarmRealtime::getMeasurePointId).collect(Collectors.toSet());

        // 4. 组装结果
        Map<Long, List<Map<String, Object>>> groupedTs = tsRows.stream()
                .collect(Collectors.groupingBy(r -> (Long) r.get("measure_point_id")));

        List<RealtimePointVO> result = new ArrayList<>();
        for (Long pid : pointIds) {
            DimMeasurePoint mp = pointMap.get(pid);
            if (mp == null) continue;
            List<Map<String, Object>> rows = groupedTs.getOrDefault(pid, List.of());
            if (rows.isEmpty()) continue;
            result.add(buildVO(mp, rows, alarmingPointIds.contains(pid)));
        }
        return result;
    }

    @Override
    public RealtimeCurveVO getRealtimeCurve(Long unitId, String deviceCode, String curveType, Integer seconds) {
        int secs = seconds != null ? Math.min(seconds, 3600) : 300;
        LocalDateTime start = LocalDateTime.now().minusSeconds(secs);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT ts, value FROM ts_measure_point WHERE unit_id = ? AND ts >= ? ORDER BY ts ASC LIMIT 1000",
                unitId, start);

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

        RealtimeCurveVO.CurveStats stats = buildStats(values);
        RealtimeCurveVO.AnomalyInfo anomaly = buildAnomalyInfo(values);

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

        List<DimMeasurePoint> allPoints = measurePointMapper.selectList(
                new LambdaQueryWrapper<DimMeasurePoint>()
                        .eq(DimMeasurePoint::getUnitId, unitId)
                        .last("LIMIT " + MAX_POINTS));
        if (allPoints.isEmpty()) {
            SnapshotVO vo = new SnapshotVO();
            vo.setUnitId(unitId);
            vo.setUnitName(unit != null ? unit.getUnitName() : "未知");
            return vo;
        }

        List<Long> pointIds = allPoints.stream().map(DimMeasurePoint::getId).collect(Collectors.toList());
        List<RealtimePointVO> pointVOs = getLatestValues(unitId, pointIds);

        int anomaly = (int) pointVOs.stream().filter(p -> Boolean.TRUE.equals(p.getAnomaly())).count();
        long alarmCount = alarmRealtimeMapper.selectCount(
                new LambdaQueryWrapper<AlarmRealtime>()
                        .eq(AlarmRealtime::getUnitId, unitId)
                        .eq(AlarmRealtime::getSuppressed, 0));
        double avgHealth = pointVOs.stream()
                .filter(p -> p.getHealthScore() != null)
                .mapToDouble(p -> p.getHealthScore().doubleValue()).average().orElse(100.0);

        SnapshotVO vo = new SnapshotVO();
        vo.setUnitId(unitId);
        vo.setUnitName(unit != null ? unit.getUnitName() : "未知");
        vo.setTotalPoints(allPoints.size());
        vo.setOnlinePoints(pointVOs.size());
        vo.setAnomalyPoints(anomaly);
        vo.setAlarmCount((int) alarmCount);
        vo.setOverallHealth(Math.round(avgHealth * 10.0) / 10.0);
        vo.setPoints(pointVOs);

        log.info("机组快照: unit={}, points={}, online={}, anomaly={}, alarms={}",
                unitId, allPoints.size(), pointVOs.size(), anomaly, alarmCount);
        return vo;
    }

    // ─── 批量查询工具 ───

    /** 一次 SQL 查出所有测点的最新 2 条数据 */
    private List<Map<String, Object>> batchQueryLastTwoValues(Long unitId, List<Long> pointIds) {
        if (pointIds.isEmpty()) return List.of();
        String inClause = pointIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String sql = "SELECT t.measure_point_id, t.ts, t.value, t.quality_flag " +
                     "FROM ts_measure_point t " +
                     "INNER JOIN (SELECT measure_point_id, MAX(ts) AS max_ts FROM ts_measure_point " +
                     "WHERE unit_id = ? AND measure_point_id IN (" + inClause + ") GROUP BY measure_point_id) latest " +
                     "ON t.measure_point_id = latest.measure_point_id AND t.ts = latest.max_ts " +
                     "UNION ALL " +
                     "SELECT t.measure_point_id, t.ts, t.value, t.quality_flag " +
                     "FROM ts_measure_point t " +
                     "INNER JOIN (SELECT measure_point_id, MAX(ts) AS max_ts FROM ts_measure_point " +
                     "WHERE unit_id = ? AND measure_point_id IN (" + inClause + ") GROUP BY measure_point_id) latest2 " +
                     "ON t.measure_point_id = latest2.measure_point_id AND t.ts < latest2.max_ts " +
                     "ORDER BY measure_point_id, ts DESC LIMIT 2";
        return jdbcTemplate.queryForList(sql, unitId, unitId);
    }

    private RealtimePointVO buildVO(DimMeasurePoint mp, List<Map<String, Object>> rows, boolean hasAlarm) {
        Map<String, Object> latest = rows.get(0);
        BigDecimal currentValue = (BigDecimal) latest.get("value");

        String trend = "stable";
        BigDecimal delta = BigDecimal.ZERO;
        if (rows.size() >= 2) {
            BigDecimal prevValue = (BigDecimal) rows.get(1).get("value");
            delta = currentValue.subtract(prevValue);
            if (delta.compareTo(BigDecimal.valueOf(0.01)) > 0) trend = "up";
            else if (delta.compareTo(BigDecimal.valueOf(-0.01)) < 0) trend = "down";
        }

        boolean isAnomaly = false;
        if (mp.getRangeLow() != null && currentValue.compareTo(BigDecimal.valueOf(mp.getRangeLow())) < 0) isAnomaly = true;
        if (mp.getRangeHigh() != null && currentValue.compareTo(BigDecimal.valueOf(mp.getRangeHigh())) > 0) isAnomaly = true;

        BigDecimal health = BigDecimal.valueOf(100);
        if (mp.getRangeHigh() != null && mp.getRangeLow() != null) {
            double range = mp.getRangeHigh() - mp.getRangeLow();
            double mid = (mp.getRangeHigh() + mp.getRangeLow()) / 2;
            health = BigDecimal.valueOf(Math.max(0, 100 - Math.abs(currentValue.doubleValue() - mid) / range * 50));
        }

        RealtimePointVO vo = new RealtimePointVO();
        vo.setPointId(mp.getId());
        vo.setPointCode(mp.getPointCode());
        vo.setPointName(mp.getPointName());
        vo.setCurrentValue(currentValue);
        vo.setUnit(mp.getUnit());
        vo.setSampleTime(latest.get("ts").toString());
        vo.setQualityFlag((Integer) latest.get("quality_flag"));
        vo.setTrend(trend);
        vo.setDelta(delta);
        vo.setAnomaly(isAnomaly);
        vo.setAlarmLevel(hasAlarm ? 1 : null);
        vo.setHealthScore(health.setScale(1, RoundingMode.HALF_UP));
        return vo;
    }

    private RealtimeCurveVO.CurveStats buildStats(List<BigDecimal> values) {
        RealtimeCurveVO.CurveStats stats = new RealtimeCurveVO.CurveStats();
        if (!values.isEmpty()) {
            stats.setMin(values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            stats.setMax(values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            stats.setAvg(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP));
            if (values.size() >= 5) {
                BigDecimal first5 = avgOf(values.subList(0, 5));
                BigDecimal last5 = avgOf(values.subList(values.size() - 5, values.size()));
                BigDecimal diff = last5.subtract(first5);
                if (diff.compareTo(BigDecimal.valueOf(0.5)) > 0) stats.setTrend("up");
                else if (diff.compareTo(BigDecimal.valueOf(-0.5)) < 0) stats.setTrend("down");
                else stats.setTrend("stable");
            }
        }
        return stats;
    }

    private RealtimeCurveVO.AnomalyInfo buildAnomalyInfo(List<BigDecimal> values) {
        RealtimeCurveVO.AnomalyInfo info = new RealtimeCurveVO.AnomalyInfo();
        if (values.size() >= 10) {
            BigDecimal mean = avgOf(values);
            double variance = values.stream().mapToDouble(v -> Math.pow(v.subtract(mean).doubleValue(), 2)).average().orElse(0);
            BigDecimal upper = mean.add(BigDecimal.valueOf(Math.sqrt(variance) * 3));
            long count = values.stream().filter(v -> v.compareTo(upper) > 0).count();
            info.setHasAnomaly(count > 0);
            info.setAnomalyCount((int) count);
            info.setDescription(count > 0 ? "检测到 " + count + " 个异常数据点" : "数据正常");
        } else {
            info.setHasAnomaly(false);
            info.setDescription("数据点不足无法检测");
        }
        return info;
    }

    private BigDecimal avgOf(List<BigDecimal> list) {
        return list.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(list.size()), 6, RoundingMode.HALF_UP);
    }
}
