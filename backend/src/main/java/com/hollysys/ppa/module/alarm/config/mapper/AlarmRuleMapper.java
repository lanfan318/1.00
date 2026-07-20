package com.hollysys.ppa.module.alarm.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hollysys.ppa.module.alarm.config.entity.AlarmRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报警规则明细 Mapper
 *
 * @author PPA Team
 */
@Mapper
public interface AlarmRuleMapper extends BaseMapper<AlarmRule> {
}
