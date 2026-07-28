package com.hollysys.ppa.module.history.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.module.history.entity.HistoryImportTask;
import com.hollysys.ppa.module.history.mapper.HistoryImportTaskMapper;
import com.hollysys.ppa.module.history.service.HistoryImportExecutor;
import com.hollysys.ppa.module.history.service.HistoryImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 历史数据导入 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryImportServiceImpl implements HistoryImportService {

    private final HistoryImportTaskMapper taskMapper;
    private final HistoryImportExecutor executor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HistoryImportTask importAsync(MultipartFile file, String operator) {
        HistoryImportTask task = new HistoryImportTask();
        task.setTaskName("导入_" + file.getOriginalFilename());
        task.setOriginalFileName(file.getOriginalFilename());
        task.setFileSize(file.getSize());
        task.setStatus(0);
        task.setStartTime(LocalDateTime.now());
        task.setOperator(operator);
        taskMapper.insert(task);

        // 通过独立 Bean 异步执行（避免 this 自调用绕过 @Async 代理）
        executor.execute(task.getId(), file);

        return task;
    }

    @Override
    public Page<HistoryImportTask> listTasks(Integer page, Integer size) {
        Page<HistoryImportTask> p = new Page<>(page, size);
        return taskMapper.selectPage(p,
                new LambdaQueryWrapper<HistoryImportTask>()
                        .orderByDesc(HistoryImportTask::getCreatedAt));
    }
}
