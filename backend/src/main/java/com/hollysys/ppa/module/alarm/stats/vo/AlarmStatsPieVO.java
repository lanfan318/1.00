package com.hollysys.ppa.module.alarm.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 饼图统计 VO
 *
 * @author PPA Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "饼图统计项")
public class AlarmStatsPieVO {

    @Schema(description = "名称（专业名/等级名/类型名）")
    private String name;

    @Schema(description = "数量")
    private Long count;

    @Schema(description = "占比（%）")
    private Double percentage;
}
