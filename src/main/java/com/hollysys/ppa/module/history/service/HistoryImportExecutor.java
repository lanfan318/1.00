package com.hollysys.ppa.module.history.service;

import cn.hutool.core.io.FileUtil;
import com.hollysys.ppa.module.history.entity.HistoryImportTask;
import com.hollysys.ppa.module.history.mapper.HistoryImportTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;

/**
 * 历史数据异步导入执行器（独立 Bean，支持 @Async 代理）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryImportExecutor {

    private final HistoryImportTaskMapper taskMapper;

    @Async
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long taskId, MultipartFile file) {
        HistoryImportTask task = taskMapper.selectById(taskId);
        if (task == null) return;

        try {
            task.setStatus(1);
            taskMapper.updateById(task);

            File tempFile = FileUtil.createTempFile(".zip", true);
            file.transferTo(tempFile);

            // TODO: 实际解析 ZIP 中的 Excel/CSV 并批量插入 ts_measure_point
            long total = 0;
            long success = 0;

            task.setTotalRecords(total);
            task.setSuccessRecords(success);
            task.setStatus(2);
            task.setEndTime(LocalDateTime.now());
            taskMapper.updateById(task);

            FileUtil.del(tempFile);
            log.info("异步导入完成: taskId={}", taskId);
        } catch (Exception e) {
            log.error("异步导入失败: taskId={}", taskId, e);
            task.setStatus(3);
            task.setErrorDetail(e.getMessage());
            task.setEndTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }
}
