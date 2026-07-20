package com.hollysys.ppa.module.history.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.history.entity.HistoryImportTask;
import com.hollysys.ppa.module.history.service.HistoryImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 历史数据导入 Controller
 *
 * @author PPA Team
 */
@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "历史数据导入", description = "ZIP/Excel 异步导入 + 任务查询")
public class HistoryImportController {

    private final HistoryImportService historyImportService;

    @PostMapping("/import")
    @Operation(summary = "导入历史数据 ZIP/Excel（异步）")
    public R<HistoryImportTask> importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "system") String operator) {
        log.info("历史数据导入: file={}", file.getOriginalFilename());
        return R.ok(historyImportService.importAsync(file, operator));
    }

    @GetMapping("/import/tasks")
    @Operation(summary = "查询导入任务列表")
    public R<Page<HistoryImportTask>> listTasks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.ok(historyImportService.listTasks(page, size));
    }
}
