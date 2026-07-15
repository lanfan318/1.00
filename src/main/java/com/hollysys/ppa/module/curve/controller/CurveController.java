package com.hollysys.ppa.module.curve.controller;

import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.curve.entity.HealthScore;
import com.hollysys.ppa.module.curve.service.CurveService;
import com.hollysys.ppa.module.curve.vo.CurveDataVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 工况曲线 Controller
 *
 * @author PPA Team
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "工况曲线", description = "工况曲线查询 / 设备健康评分")
public class CurveController {

    private final CurveService curveService;

    @GetMapping("/{unit}/{device}/curve")
    @Operation(summary = "查询工况曲线")
    public R<CurveDataVO> getCurve(
            @PathVariable Long unit,
            @PathVariable String device,
            @Parameter(description = "曲线类型: fd(风道) / sb(设备)") @RequestParam(defaultValue = "sb") String type,
            @Parameter(description = "时间范围: 1h/6h/24h/7d") @RequestParam(defaultValue = "24h") String range) {
        return R.ok(curveService.getCurve(unit, device, type, range));
    }

    @GetMapping("/{unit}/{device}/health")
    @Operation(summary = "查询设备健康评分")
    public R<HealthScore> getHealth(
            @PathVariable Long unit,
            @PathVariable String device) {
        return R.ok(curveService.getHealth(unit, device));
    }
}
