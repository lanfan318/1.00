package com.hollysys.ppa.module.ai.controller;

import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.ai.dto.AiAnalysisDTO;
import com.hollysys.ppa.module.ai.service.AiEngineService;
import com.hollysys.ppa.module.ai.vo.AiAnalysisVO;
import com.hollysys.ppa.module.ai.vo.PredictResultVO;
import com.hollysys.ppa.module.ai.vo.RootCauseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内置 AI 引擎 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 引擎", description = "内置 AI 分析：报警诊断 / 设备预测 / 根因分析")
public class AiController {

    private final AiEngineService aiEngineService;

    @PostMapping("/analyze")
    @Operation(summary = "通用 AI 分析入口")
    public R<AiAnalysisVO> analyze(@RequestBody AiAnalysisDTO dto) {
        log.info("AI 分析: type={}", dto.getAnalysisType());
        return R.ok(aiEngineService.analyze(dto));
    }

    @PostMapping("/alarm/{id}/analyze")
    @Operation(summary = "AI 分析报警（诊断 + 建议）")
    public R<AiAnalysisVO> analyzeAlarm(@PathVariable Long id) {
        return R.ok(aiEngineService.analyzeAlarm(id));
    }

    @PostMapping("/equipment/{unit}/{device}/predict")
    @Operation(summary = "设备状态预测")
    public R<PredictResultVO> predictEquipment(
            @PathVariable Long unit, @PathVariable String device) {
        return R.ok(aiEngineService.predictEquipment(unit, device));
    }

    @PostMapping("/alarm/{id}/root-cause")
    @Operation(summary = "报警根因分析")
    public R<RootCauseVO> rootCause(@PathVariable Long id) {
        return R.ok(aiEngineService.analyzeRootCause(id));
    }

    @GetMapping("/alarm/{id}/history")
    @Operation(summary = "查询报警的历史 AI 分析结果")
    public R<List<AiAnalysisVO>> getHistory(@PathVariable Long id) {
        return R.ok(aiEngineService.getHistory(id));
    }
}
