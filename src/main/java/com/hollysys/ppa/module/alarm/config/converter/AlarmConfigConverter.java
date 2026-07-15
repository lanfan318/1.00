package com.hollysys.ppa.module.alarm.config.converter;

import com.hollysys.ppa.module.alarm.config.dto.AlarmConfigSaveDTO;
import com.hollysys.ppa.module.alarm.config.entity.AlarmConfig;
import com.hollysys.ppa.module.alarm.config.entity.AlarmRule;
import com.hollysys.ppa.module.alarm.config.vo.AlarmConfigVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 报警配置 MapStruct 转换器
 *
 * @author PPA Team
 */
@Mapper(componentModel = "spring")
public interface AlarmConfigConverter {

    AlarmConfigConverter INSTANCE = Mappers.getMapper(AlarmConfigConverter.class);

    /**
     * SaveDTO → AlarmConfig 实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    AlarmConfig toEntity(AlarmConfigSaveDTO dto);

    /**
     * AlarmConfig 实体 → VO（不含 rules）
     */
    @Mapping(target = "rules", ignore = true)
    AlarmConfigVO toVO(AlarmConfig entity);

    /**
     * SaveDTO.AlarmRuleDTO → AlarmRule 实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "configId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    AlarmRule ruleDtoToEntity(AlarmConfigSaveDTO.AlarmRuleDTO dto);

    /**
     * AlarmRule 实体 → VO
     */
    AlarmConfigVO.AlarmRuleVO ruleToVO(AlarmRule entity);

    List<AlarmRule> ruleDtoListToEntityList(List<AlarmConfigSaveDTO.AlarmRuleDTO> dtos);

    List<AlarmConfigVO.AlarmRuleVO> ruleListToVOList(List<AlarmRule> entities);
}
