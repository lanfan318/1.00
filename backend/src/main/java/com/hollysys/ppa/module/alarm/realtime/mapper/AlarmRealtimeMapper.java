package com.hollysys.ppa.module.alarm.realtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.module.alarm.realtime.dto.AlarmRealtimeQueryDTO;
import com.hollysys.ppa.module.alarm.realtime.entity.AlarmRealtime;
import com.hollysys.ppa.module.alarm.realtime.vo.AlarmRealtimeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 实时报警 Mapper
 *
 * @author PPA Team
 */
@Mapper
public interface AlarmRealtimeMapper extends BaseMapper<AlarmRealtime> {

    /**
     * 分页查询实时报警（关联维度表）
     */
    IPage<AlarmRealtimeVO> selectPageWithNames(Page<?> page, @Param("query") AlarmRealtimeQueryDTO query);

    /**
     * 统计各等级数量
     */
    Long countByLevel(@Param("alarmLevel") Integer alarmLevel);

    /**
     * 查询某等级 Top N 最新报警
     */
    java.util.List<AlarmRealtimeVO> selectTopByLevel(@Param("alarmLevel") Integer alarmLevel, @Param("limit") int limit);
}
