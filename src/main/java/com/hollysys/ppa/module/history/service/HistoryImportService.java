package com.hollysys.ppa.module.history.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.module.history.entity.HistoryImportTask;
import org.springframework.web.multipart.MultipartFile;

/**
 * 历史数据导入 Service
 *
 * @author PPA Team
 */
public interface HistoryImportService {

    /**
     * 异步导入 ZIP/Excel
     */
    HistoryImportTask importAsync(MultipartFile file, String operator);

    /**
     * 查询任务列表
     */
    Page<HistoryImportTask> listTasks(Integer page, Integer size);
}
