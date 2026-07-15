package com.hollysys.ppa.module.history.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 历史数据导入任务表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_history_import_task")
@Schema(description = "历史数据导入任务")
public class HistoryImportTask extends BaseEntity {

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "原始文件名")
    private String originalFileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "状态: 0-等待 1-处理中 2-成功 3-失败")
    private Integer status;

    @Schema(description = "总记录数")
    private Long totalRecords;

    @Schema(description = "成功记录数")
    private Long successRecords;

    @Schema(description = "失败记录数")
    private Long failRecords;

    @Schema(description = "错误详情")
    private String errorDetail;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "操作人")
    private String operator;
}
