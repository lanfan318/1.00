package com.hollysys.ppa.module.alarm.realtime.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.common.BusinessException;
import com.hollysys.ppa.module.alarm.realtime.dto.AlarmRealtimeQueryDTO;
import com.hollysys.ppa.module.alarm.realtime.entity.AlarmRealtime;
import com.hollysys.ppa.module.alarm.realtime.mapper.AlarmRealtimeMapper;
import com.hollysys.ppa.module.alarm.realtime.service.AlarmRealtimeService;
import com.hollysys.ppa.module.alarm.realtime.vo.AlarmLevelGroupVO;
import com.hollysys.ppa.module.alarm.realtime.vo.AlarmRealtimeVO;
import com.hollysys.ppa.module.alarm.suppress.entity.AlarmSuppress;
import com.hollysys.ppa.module.alarm.suppress.mapper.AlarmSuppressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 实时报警 Service 实现
 *
 * @author PPA Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmRealtimeServiceImpl implements AlarmRealtimeService {

    private final AlarmRealtimeMapper alarmRealtimeMapper;
    private final AlarmSuppressMapper alarmSuppressMapper;

    @Override
    public IPage<AlarmRealtimeVO> page(AlarmRealtimeQueryDTO query) {
        Page<AlarmRealtimeVO> page = new Page<>(query.getPage(), query.getSize());
        return alarmRealtimeMapper.selectPageWithNames(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, String operator) {
        AlarmRealtime alarm = alarmRealtimeMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException("报警不存在: id=" + id);
        }
        if (alarm.getConfirmed() == 1) {
            throw new BusinessException("该报警已确认");
        }
        alarmRealtimeMapper.update(null,
                new LambdaUpdateWrapper<AlarmRealtime>()
                        .eq(AlarmRealtime::getId, id)
                        .set(AlarmRealtime::getConfirmed, 1)
                        .set(AlarmRealtime::getConfirmBy, operator)
                        .set(AlarmRealtime::getConfirmTime, LocalDateTime.now()));
        log.info("确认报警: id={}, operator={}", id, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suppress(Long id, String reason, String operator) {
        AlarmRealtime alarm = alarmRealtimeMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException("报警不存在: id=" + id);
        }
        if (alarm.getSuppressed() == 1) {
            throw new BusinessException("该报警已抑制");
        }

        // 更新报警抑制状态
        alarmRealtimeMapper.update(null,
                new LambdaUpdateWrapper<AlarmRealtime>()
                        .eq(AlarmRealtime::getId, id)
                        .set(AlarmRealtime::getSuppressed, 1)
                        .set(AlarmRealtime::getSuppressReason, reason));

        // 记录抑制日志
        AlarmSuppress suppress = new AlarmSuppress();
        suppress.setAlarmId(id);
        suppress.setConfigId(alarm.getConfigId());
        suppress.setMeasurePointId(alarm.getMeasurePointId());
        suppress.setSuppressType(1); // 手动抑制
        suppress.setReason(reason);
        suppress.setStartTime(LocalDateTime.now());
        suppress.setOperator(operator);
        alarmSuppressMapper.insert(suppress);

        log.info("抑制报警: id={}, reason={}, operator={}", id, reason, operator);
    }

    @Override
    public AlarmLevelGroupVO getLevelGroups() {
        AlarmLevelGroupVO vo = new AlarmLevelGroupVO();

        vo.setLevel1(buildGroup(1, 5));
        vo.setLevel2(buildGroup(2, 5));
        vo.setLevel3(buildGroup(3, 5));

        return vo;
    }

    private AlarmLevelGroupVO.LevelGroup buildGroup(int level, int limit) {
        AlarmLevelGroupVO.LevelGroup group = new AlarmLevelGroupVO.LevelGroup();
        group.setLevel(level);
        group.setCount(alarmRealtimeMapper.countByLevel(level));
        group.setTopList(alarmRealtimeMapper.selectTopByLevel(level, limit));
        return group;
    }
}
