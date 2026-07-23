package com.hollysys.ppa.module.realtime.controller;

import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.realtime.service.RealtimeDataService;
import com.hollysys.ppa.module.realtime.vo.RealtimeCurveVO;
import com.hollysys.ppa.module.realtime.vo.RealtimePointVO;
import com.hollysys.ppa.module.realtime.vo.SnapshotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 实时数据 Controller
 * 前端轮询此接口实现仪表盘数据的动态刷新
 */
@Slf4j
@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
@Tag(name = "实时数据", description = "测点实时值 / 曲线实时数据 / 机组快照 —— 前端轮询动态刷新")
public class RealtimeDataController {

    private final RealtimeDataService realtimeDataService;

    @GetMapping("/values")
    @Operation(summary = "测点实时值", description = "查询指定测点的最新值，含趋势/异常/健康评分。前端每 3-5 秒轮询一次")
    public R<List<RealtimePointVO>> getValues(
            @Parameter(description = "机组ID") @RequestParam Long unitId,
            @Parameter(description = "测点ID列表，逗号分隔，不传则返回全部") @RequestParam(required = false) List<Long> pointIds) {
        return R.ok(realtimeDataService.getLatestValues(unitId, pointIds));
    }

    @GetMapping("/curve")
    @Operation(summary = "实时曲线数据", description = "获取最近 N 秒的曲线数据 + 统计摘要 + 异常标记。前端每 5-10 秒轮询一次")
    public R<RealtimeCurveVO> getCurve(
            @Parameter(description = "机组ID") @RequestParam Long unitId,
            @Parameter(description = "设备编码") @RequestParam String deviceCode,
            @Parameter(description = "曲线类型: fd/sb") @RequestParam(defaultValue = "sb") String curveType,
            @Parameter(description = "时间范围（秒），默认300，最大3600") @RequestParam(defaultValue = "300") Integer seconds) {
        return R.ok(realtimeDataService.getRealtimeCurve(unitId, deviceCode, curveType, seconds));
    }

    @GetMapping("/snapshot")
    @Operation(summary = "机组快照", description = "仪表盘总览：所有测点实时值 + 在线率 + 异常数 + 报警数 + 整体健康分")
    public R<SnapshotVO> getSnapshot(
            @Parameter(description = "机组ID") @RequestParam Long unitId) {
        return R.ok(realtimeDataService.getSnapshot(unitId));
    }
}
