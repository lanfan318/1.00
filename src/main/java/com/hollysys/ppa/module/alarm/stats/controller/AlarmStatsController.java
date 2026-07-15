package com.hollysys.ppa.module.alarm.stats.controller;

import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.alarm.stats.service.AlarmStatsService;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsOverviewVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsPieVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsRankVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报警统计 Controller
 *
 * @author PPA Team
 */
@Slf4j
@RestController
@RequestMapping("/api/alarm/stats")
@RequiredArgsConstructor
@Tag(name = "报警统计", description = "统计概览 / 占比饼图 / Top10 / 日趋势（Redis 缓存 5 分钟）")
public class AlarmStatsController {

    private final AlarmStatsService alarmStatsService;

    @GetMapping("/overview")
    @Operation(summary = "统计概览", description = "range: week/month/quarter/year")
    public R<AlarmStatsOverviewVO> overview(@RequestParam(defaultValue = "month") String range) {
        return R.ok(alarmStatsService.overview(range));
    }

    @GetMapping("/specialty")
    @Operation(summary = "专业占比饼图")
    public R<List<AlarmStatsPieVO>> specialty(@RequestParam(defaultValue = "month") String range) {
        return R.ok(alarmStatsService.specialtyPie(range));
    }

    @GetMapping("/level")
    @Operation(summary = "级别占比饼图")
    public R<List<AlarmStatsPieVO>> level(@RequestParam(defaultValue = "month") String range) {
        return R.ok(alarmStatsService.levelPie(range));
    }

    @GetMapping("/type")
    @Operation(summary = "类型占比饼图")
    public R<List<AlarmStatsPieVO>> type(@RequestParam(defaultValue = "month") String range) {
        return R.ok(alarmStatsService.typePie(range));
    }

    @GetMapping("/top-frequency")
    @Operation(summary = "频次 Top10")
    public R<List<AlarmStatsRankVO>> topFrequency(@RequestParam(defaultValue = "month") String range) {
        return R.ok(alarmStatsService.topFrequency(range));
    }

    @GetMapping("/top-duration")
    @Operation(summary = "时长 Top10")
    public R<List<AlarmStatsRankVO>> topDuration(@RequestParam(defaultValue = "month") String range) {
        return R.ok(alarmStatsService.topDuration(range));
    }

    @GetMapping("/daily-trend")
    @Operation(summary = "属性分布日趋势柱状图")
    public R<List<AlarmStatsTrendVO>> dailyTrend(@RequestParam(defaultValue = "month") String range) {
        return R.ok(alarmStatsService.dailyTrend(range));
    }
}
