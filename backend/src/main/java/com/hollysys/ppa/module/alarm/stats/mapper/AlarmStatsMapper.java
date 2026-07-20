package com.hollysys.ppa.module.alarm.stats.mapper;

import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsPieVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsRankVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsTrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报警统计 Mapper（纯聚合查询）
 *
 * @author PPA Team
 */
@Mapper
public interface AlarmStatsMapper {

    /** 时间段内报警总数 */
    Long countTotal(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 时间段内未确认数量 */
    Long countUnconfirmed(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 时间段内已抑制数量 */
    Long countSuppressed(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 平均处理时长（分钟） */
    Double avgHandleMinutes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 专业占比 */
    List<AlarmStatsPieVO> specialtyPie(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 级别占比 */
    List<AlarmStatsPieVO> levelPie(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 类型占比 */
    List<AlarmStatsPieVO> typePie(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 频次 Top10（历史表聚合） */
    List<AlarmStatsRankVO> topFrequency(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("limit") int limit);

    /** 时长 Top10（历史表聚合） */
    List<AlarmStatsRankVO> topDuration(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("limit") int limit);

    /** 日趋势（按天+等级分组） */
    List<AlarmStatsTrendVO> dailyTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
