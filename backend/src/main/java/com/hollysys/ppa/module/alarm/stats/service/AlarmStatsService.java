package com.hollysys.ppa.module.alarm.stats.service;

import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsOverviewVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsPieVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsRankVO;
import com.hollysys.ppa.module.alarm.stats.vo.AlarmStatsTrendVO;

import java.util.List;

/**
 * 报警统计 Service
 *
 * @author PPA Team
 */
public interface AlarmStatsService {

    AlarmStatsOverviewVO overview(String range);

    List<AlarmStatsPieVO> specialtyPie(String range);

    List<AlarmStatsPieVO> levelPie(String range);

    List<AlarmStatsPieVO> typePie(String range);

    List<AlarmStatsRankVO> topFrequency(String range);

    List<AlarmStatsRankVO> topDuration(String range);

    List<AlarmStatsTrendVO> dailyTrend(String range);
}
