package com.hollysys.ppa.module.alarm.config.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.alarm.config.dto.AlarmConfigQueryDTO;
import com.hollysys.ppa.module.alarm.config.dto.AlarmConfigSaveDTO;
import com.hollysys.ppa.module.alarm.config.service.AlarmConfigService;
import com.hollysys.ppa.module.alarm.config.vo.AlarmConfigListVO;
import com.hollysys.ppa.module.alarm.config.vo.AlarmConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 报警配置 Controller
 *
 * @author PPA Team
 */
@Slf4j
@RestController
@RequestMapping("/api/alarm/config")
@RequiredArgsConstructor
@Tag(name = "报警配置", description = "报警规则 CRUD / 导入导出 / 启用禁用")
public class AlarmConfigController {

    private final AlarmConfigService alarmConfigService;

    @GetMapping
    @Operation(summary = "分页查询报警配置")
    public R<IPage<AlarmConfigListVO>> page(@Valid AlarmConfigQueryDTO query) {
        log.info("分页查询报警配置: page={}, size={}", query.getPage(), query.getSize());
        return R.ok(alarmConfigService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询报警配置详情（含规则明细）")
    public R<AlarmConfigVO> getById(@PathVariable Long id) {
        log.info("查询报警配置详情: id={}", id);
        return R.ok(alarmConfigService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增报警配置（含多行规则子表）")
    public R<Void> save(@Valid @RequestBody AlarmConfigSaveDTO dto) {
        log.info("新增报警配置: code={}", dto.getRuleCode());
        alarmConfigService.save(dto);
        return R.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑报警配置")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AlarmConfigSaveDTO dto) {
        log.info("编辑报警配置: id={}", id);
        alarmConfigService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报警配置（级联删除规则明细）")
    public R<Void> delete(@PathVariable Long id) {
        log.info("删除报警配置: id={}", id);
        alarmConfigService.delete(id);
        return R.ok();
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除报警配置")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        log.info("批量删除报警配置: ids={}", ids);
        alarmConfigService.batchDelete(ids);
        return R.ok();
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用报警配置")
    public R<Void> enable(@PathVariable Long id) {
        log.info("启用报警配置: id={}", id);
        alarmConfigService.enable(id);
        return R.ok();
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "禁用报警配置")
    public R<Void> disable(@PathVariable Long id) {
        log.info("禁用报警配置: id={}", id);
        alarmConfigService.disable(id);
        return R.ok();
    }

    @GetMapping("/export")
    @Operation(summary = "导出报警配置为 Excel")
    public void exportExcel(AlarmConfigQueryDTO query, HttpServletResponse response) {
        log.info("导出报警配置");
        alarmConfigService.exportExcel(query, response);
    }

    @PostMapping("/import")
    @Operation(summary = "导入 Excel/ZIP（报警配置）")
    public R<Void> importFile(
            @Parameter(description = "Excel 文件") @RequestParam("file") MultipartFile file) throws IOException {
        log.info("导入报警配置: filename={}", file.getOriginalFilename());
        alarmConfigService.importFile(file.getBytes(), file.getOriginalFilename());
        return R.ok();
    }
}
