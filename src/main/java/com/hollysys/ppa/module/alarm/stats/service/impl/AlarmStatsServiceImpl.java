package com.hollysys.ppa.module.alarm.stats.service.impl;

import com.hollysys.ppa.module.alarm.stats.mapper.AlarmStatsMapper;
import com.hollysys.ppa.module.alarm.stats.service.AlarmStatsService;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsOverviewVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsPieVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsRankVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsTrendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报警统计 Service 实现（Redis 缓存 5 分钟）
 *
 * @author PPA Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmStatsServiceImpl implements AlarmStatsService {

    private final AlarmStatsMapper alarmStatsMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime[] resolveRange(String range) {
        LocalDate today = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end = LocalDateTime.of(today, LocalTime.MAX);
        switch (range) {
            case "week":
                start = LocalDateTime.of(today.minusDays(7), LocalTime.MIN);
                break;
            case "month":
                start = LocalDateTime.of(today.minusMonths(1), LocalTime.MIN);
                break;
            case "quarter":
                start = LocalDateTime.of(today.minusMonths(3), LocalTime.MIN);
                break;
            case "year":
                start = LocalDateTime.of(today.minusYears(1), LocalTime.MIN);
                break;
            default:
                start = LocalDateTime.of(today.minusMonths(1), LocalTime.MIN);
        }
        return new LocalDateTime[]{start, end};
    }

    @Override
    @Cacheable(value = "alarm:stats:overview", key = "#range", unless = "#result == null")
    public AlarmStatsOverviewVO overview(String range) {
        log.info("统计概览: range={}", range);
        LocalDateTime[] r = resolveRange(range);
        AlarmStatsOverviewVO vo = new AlarmStatsOverviewVO();
        vo.setTotalCount(alarmStatsMapper.countTotal(r[0], r[1]));
        vo.setUnconfirmedCount(alarmStatsMapper.countUnconfirmed(r[0], r[1]));
        vo.setSuppressedCount(alarmStatsMapper.countSuppressed(r[0], r[1]));
        Double avgMin = alarmStatsMapper.avgHandleMinutes(r[0], r[1]);
        vo.setAvgHandleMinutes(avgMin != null ? BigDecimal.valueOf(avgMin).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        vo.setChangeRate(BigDecimal.ZERO); // 环比需额外查询上期数据
        vo.setRangeStart(DATE_FMT.format(r[0]));
        vo.setRangeEnd(DATE_FMT.format(r[1]));
        return vo;
    }

    @Override
    @Cacheable(value = "alarm:stats:specialty", key = "#range", unless = "#result == null || #result.isEmpty()")
    public List<AlarmStatsPieVO> specialtyPie(String range) {
        log.info("专业占比: range={}", range);
        LocalDateTime[] r = resolveRange(range);
        return alarmStatsMapper.specialtyPie(r[0], r[1]);
    }

    @Override
    @Cacheable(value = "alarm:stats:level", key = "#range", unless = "#result == null || #result.isEmpty()")
    public List<AlarmStatsPieVO> levelPie(String range) {
        log.info("级别占比: range={}", range);
        LocalDateTime[] r = resolveRange(range);
        return alarmStatsMapper.levelPie(r[0], r[1]);
    }

    @Override
    @Cacheable(value = "alarm:stats:type", key = "#range", unless = "#result == null || #result.isEmpty()")
    public List<AlarmStatsPieVO> typePie(String range) {
        log.info("类型占比: range={}", range);
        LocalDateTime[] r = resolveRange(range);
        return alarmStatsMapper.typePie(r[0], r[1]);
    }

    @Override
    @Cacheable(value = "alarm:stats:topFreq", key = "#range", unless = "#result == null || #result.isEmpty()")
    public List<AlarmStatsRankVO> topFrequency(String range) {
        log.info("频次Top10: range={}", range);
        LocalDateTime[] r = resolveRange(range);
        return alarmStatsMapper.topFrequency(r[0], r[1], 10);
    }

    @Override
    @Cacheable(value = "alarm:stats:topDur", key = "#range", unless = "#result == null || #result.isEmpty()")
    public List<AlarmStatsRankVO> topDuration(String range) {
        log.info("时长Top10: range={}", range);
        LocalDateTime[] r = resolveRange(range);
        return alarmStatsMapper.topDuration(r[0], r[1], 10);
    }

    @Override
    @Cacheable(value = "alarm:stats:dailyTrend", key = "#range", unless = "#result == null || #result.isEmpty()")
    public List<AlarmStatsTrendVO> dailyTrend(String range) {
        log.info("日趋势: range={}", range);
        LocalDateTime[] r = resolveRange(range);
        return alarmStatsMapper.dailyTrend(r[0], r[1]);
    }
}
