package com.hollysys.ppa.module.curve.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hollysys.ppa.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备健康评分表
 *
 * @author PPA Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_health_score")
@Schema(description = "健康评分")
public class HealthScore extends BaseEntity {

    @Schema(description = "机组ID")
    private Long unitId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "健康评分 (0-100)")
    private BigDecimal score;

    @Schema(description = "评分时间")
    private LocalDateTime scoreTime;

    @Schema(description = "评分依据/详情 JSON")
    private String detail;
}
