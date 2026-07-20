package com.hollysys.ppa.module.history.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.module.history.entity.HistoryImportTask;
import com.hollysys.ppa.module.history.mapper.HistoryImportTaskMapper;
import com.hollysys.ppa.module.history.service.HistoryImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;

/**
 * 历史数据导入 Service 实现
 *
 * @author PPA Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryImportServiceImpl implements HistoryImportService {

    private final HistoryImportTaskMapper taskMapper;

    @Override
    public HistoryImportTask importAsync(MultipartFile file, String operator) {
        HistoryImportTask task = new HistoryImportTask();
        task.setTaskName("导入_" + file.getOriginalFilename());
        task.setOriginalFileName(file.getOriginalFilename());
        task.setFileSize(file.getSize());
        task.setStatus(0); // 等待
        task.setStartTime(LocalDateTime.now());
        task.setOperator(operator);
        taskMapper.insert(task);

        // 异步处理
        doImport(task.getId(), file);

        return task;
    }

    @Async
    public void doImport(Long taskId, MultipartFile file) {
        HistoryImportTask task = taskMapper.selectById(taskId);
        try {
            task.setStatus(1); // 处理中
            taskMapper.updateById(task);

            // 保存临时文件
            File tempFile = FileUtil.createTempFile(".zip", true);
            file.transferTo(tempFile);

            // TODO: 实际解析 ZIP 中的 Excel/CSV 并批量插入 ts_measure_point
            // 此处为骨架代码，解析逻辑根据实际文件格式实现
            long total = 0;
            long success = 0;

            task.setTotalRecords(total);
            task.setSuccessRecords(success);
            task.setStatus(2); // 成功
            task.setEndTime(LocalDateTime.now());
            taskMapper.updateById(task);

            FileUtil.del(tempFile);
            log.info("导入任务完成: taskId={}, success={}", taskId, success);
        } catch (Exception e) {
            log.error("导入任务失败: taskId={}", taskId, e);
            task.setStatus(3);
            task.setErrorDetail(e.getMessage());
            task.setEndTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    @Override
    public Page<HistoryImportTask> listTasks(Integer page, Integer size) {
        Page<HistoryImportTask> p = new Page<>(page, size);
        return taskMapper.selectPage(p,
                new LambdaQueryWrapper<HistoryImportTask>()
                        .orderByDesc(HistoryImportTask::getCreatedAt));
    }
}
