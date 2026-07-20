package com.hollysys.ppa.module.guide.controller;

import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.guide.entity.OperationGuide;
import com.hollysys.ppa.module.guide.service.OperationGuideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 操作指导 Controller（KG 转发 + 落库）
 *
 * @author PPA Team
 */
@Slf4j
@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
@Tag(name = "操作指导", description = "操作指导查询（含 KG 转发）/ 运行意见填写")
public class OperationGuideController {

    private final OperationGuideService guideService;

    @GetMapping("/{id}/operation-guide")
    @Operation(summary = "获取操作指导（本地优先，无则转发 KG 服务并落库）")
    public R<OperationGuide> getGuide(@PathVariable Long id) {
        log.info("获取操作指导: alarmId={}", id);
        return R.ok(guideService.getByAlarmId(id));
    }

    @PostMapping("/{id}/operation-guide/comment")
    @Operation(summary = "填写运行意见")
    public R<Void> addComment(
            @PathVariable Long id,
            @Parameter(description = "运行意见") @RequestParam String comment,
            @Parameter(description = "操作人") @RequestParam(defaultValue = "system") String operator) {
        log.info("填写运行意见: alarmId={}, operator={}", id, operator);
        guideService.addComment(id, comment, operator);
        return R.ok();
    }
}
