package com.hollysys.ppa.module.alarm.realtime.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hollysys.ppa.module.alarm.realtime.dto.AlarmRealtimeQueryDTO;
import com.hollysys.ppa.module.alarm.realtime.vo.AlarmLevelGroupVO;
import com.hollysys.ppa.module.alarm.realtime.vo.AlarmRealtimeVO;

/**
 * 实时报警 Service
 *
 * @author PPA Team
 */
public interface AlarmRealtimeService {

    /**
     * 分页查询实时报警
     */
    IPage<AlarmRealtimeVO> page(AlarmRealtimeQueryDTO query);

    /**
     * 确认报警
     */
    void confirm(Long id, String operator);

    /**
     * 抑制报警
     */
    void suppress(Long id, String reason, String operator);

    /**
     * 报警分级分组（一级/二级/智能预警三组卡片）
     */
    AlarmLevelGroupVO getLevelGroups();
}
