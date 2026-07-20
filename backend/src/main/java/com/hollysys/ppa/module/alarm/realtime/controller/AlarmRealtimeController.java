package com.hollysys.ppa.module.alarm.realtime.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.alarm.realtime.dto.AlarmRealtimeQueryDTO;
import com.hollysys.ppa.module.alarm.realtime.service.AlarmRealtimeService;
import com.hollysys.ppa.module.alarm.realtime.vo.AlarmLevelGroupVO;
import com.hollysys.ppa.module.alarm.realtime.vo.AlarmRealtimeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 实时报警 Controller
 *
 * @author PPA Team
 */
@Slf4j
@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
@Tag(name = "实时报警", description = "实时报警查询 / 确认 / 抑制 / 分级分组")
public class AlarmRealtimeController {

    private final AlarmRealtimeService alarmRealtimeService;

    @GetMapping("/realtime")
    @Operation(summary = "分页查询实时报警")
    public R<IPage<AlarmRealtimeVO>> page(AlarmRealtimeQueryDTO query) {
        log.info("分页查询实时报警: page={}", query.getPage());
        return R.ok(alarmRealtimeService.page(query));
    }

    @PostMapping("/realtime/{id}/confirm")
    @Operation(summary = "确认报警")
    public R<Void> confirm(
            @PathVariable Long id,
            @Parameter(description = "操作人") @RequestParam @NotBlank String operator) {
        log.info("确认报警: id={}, operator={}", id, operator);
        alarmRealtimeService.confirm(id, operator);
        return R.ok();
    }

    @PostMapping("/realtime/{id}/suppress")
    @Operation(summary = "抑制报警")
    public R<Void> suppress(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");
        String operator = body.getOrDefault("operator", "system");
        log.info("抑制报警: id={}, operator={}", id, operator);
        alarmRealtimeService.suppress(id, reason, operator);
        return R.ok();
    }

    @GetMapping("/level/groups")
    @Operation(summary = "报警分级分组（一级/二级/智能预警卡片）")
    public R<AlarmLevelGroupVO> getLevelGroups() {
        log.info("获取报警分级分组");
        return R.ok(alarmRealtimeService.getLevelGroups());
    }
}
