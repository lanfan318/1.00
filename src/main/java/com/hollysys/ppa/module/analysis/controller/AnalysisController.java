package com.hollysys.ppa.module.analysis.controller;

import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.analysis.service.AnalysisService;
import com.hollysys.ppa.module.analysis.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据分析算法 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "数据分析", description = "异常检测 / 趋势预测 / 健康评分 / 关联分析 / 实时统计")
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/anomaly")
    @Operation(summary = "异常检测（3-sigma + 移动平均）")
    public R<AnomalyResultVO> detectAnomaly(
            @RequestParam Long unitId,
            @RequestParam Long measurePointId,
            @RequestParam(defaultValue = "24h") String range) {
        return R.ok(analysisService.detectAnomaly(unitId, measurePointId, range));
    }

    @GetMapping("/trend")
    @Operation(summary = "趋势预测（线性回归）")
    public R<TrendResultVO> predictTrend(
            @RequestParam Long unitId,
            @RequestParam Long measurePointId,
            @RequestParam(defaultValue = "24") Integer forecastHours) {
        return R.ok(analysisService.predictTrend(unitId, measurePointId, forecastHours));
    }

    @GetMapping("/health/{unit}/{device}")
    @Operation(summary = "设备健康评分（加权算法）")
    public R<HealthCalcResultVO> calculateHealth(
            @PathVariable Long unit, @PathVariable String device) {
        return R.ok(analysisService.calculateHealth(unit, device));
    }

    @GetMapping("/correlation/{alarmId}")
    @Operation(summary = "报警关联分析（时间窗口）")
    public R<CorrelationResultVO> analyzeCorrelation(
            @PathVariable Long alarmId,
            @RequestParam(defaultValue = "30") Integer windowMinutes) {
        return R.ok(analysisService.analyzeCorrelation(alarmId, windowMinutes));
    }

    @GetMapping("/realtime-stats")
    @Operation(summary = "实时数据统计")
    public R<List<RealTimeStatsVO>> realTimeStats(@RequestParam Long unitId) {
        return R.ok(analysisService.realTimeStats(unitId));
    }
}
