package com.hollysys.ppa.module.alarm.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.module.alarm.config.dto.AlarmConfigQueryDTO;
import com.hollysys.ppa.module.alarm.config.entity.AlarmConfig;
import com.hollysys.ppa.module.alarm.config.vo.AlarmConfigListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 报警配置 Mapper
 *
 * @author PPA Team
 */
@Mapper
public interface AlarmConfigMapper extends BaseMapper<AlarmConfig> {

    /**
     * 分页查询报警配置列表（关联维度表获取名称）
     */
    IPage<AlarmConfigListVO> selectPageWithNames(Page<?> page, @Param("query") AlarmConfigQueryDTO query);

    /**
     * 根据配置ID逻辑删除关联的所有规则明细
     */
    int softDeleteRulesByConfigId(@Param("configId") Long configId);

    /**
     * 批量更新启用状态
     */
    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);
}
