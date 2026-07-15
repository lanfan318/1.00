package com.hollysys.ppa.module.history.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hollysys.ppa.module.history.entity.HistoryImportTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 历史数据导入任务 Mapper
 *
 * @author PPA Team
 */
@Mapper
public interface HistoryImportTaskMapper extends BaseMapper<HistoryImportTask> {
}
