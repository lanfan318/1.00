package com.hollysys.ppa.module.alarm.config.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.common.BusinessException;
import com.hollysys.ppa.module.alarm.config.converter.AlarmConfigConverter;
import com.hollysys.ppa.module.alarm.config.dto.AlarmConfigQueryDTO;
import com.hollysys.ppa.module.alarm.config.dto.AlarmConfigSaveDTO;
import com.hollysys.ppa.module.alarm.config.entity.AlarmConfig;
import com.hollysys.ppa.module.alarm.config.entity.AlarmRule;
import com.hollysys.ppa.module.alarm.config.mapper.AlarmConfigMapper;
import com.hollysys.ppa.module.alarm.config.mapper.AlarmRuleMapper;
import com.hollysys.ppa.module.alarm.config.service.AlarmConfigService;
import com.hollysys.ppa.module.alarm.config.vo.AlarmConfigListVO;
import com.hollysys.ppa.module.alarm.config.vo.AlarmConfigVO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 报警配置 Service 实现
 *
 * @author PPA Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmConfigServiceImpl implements AlarmConfigService {

    private final AlarmConfigMapper alarmConfigMapper;
    private final AlarmRuleMapper alarmRuleMapper;
    private final AlarmConfigConverter converter;

    @Override
    public IPage<AlarmConfigListVO> page(AlarmConfigQueryDTO query) {
        Page<AlarmConfigListVO> page = new Page<>(query.getPage(), query.getSize());
        return alarmConfigMapper.selectPageWithNames(page, query);
    }

    @Override
    public AlarmConfigVO getById(Long id) {
        AlarmConfig config = alarmConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("报警配置不存在: id=" + id);
        }
        List<AlarmRule> rules = alarmRuleMapper.selectList(
                new LambdaQueryWrapper<AlarmRule>()
                        .eq(AlarmRule::getConfigId, id)
                        .orderByAsc(AlarmRule::getSortOrder));
        AlarmConfigVO vo = converter.toVO(config);
        vo.setRules(converter.ruleListToVOList(rules));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(AlarmConfigSaveDTO dto) {
        // 校验编码唯一性
        Long count = alarmConfigMapper.selectCount(
                new LambdaQueryWrapper<AlarmConfig>()
                        .eq(AlarmConfig::getRuleCode, dto.getRuleCode()));
        if (count > 0) {
            throw new BusinessException("规则编码已存在: " + dto.getRuleCode());
        }

        AlarmConfig config = converter.toEntity(dto);
        alarmConfigMapper.insert(config);
        log.info("新增报警配置: id={}, code={}", config.getId(), config.getRuleCode());

        // 批量插入规则明细
        saveRules(config.getId(), dto.getRules());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AlarmConfigSaveDTO dto) {
        AlarmConfig config = alarmConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("报警配置不存在: id=" + id);
        }

        // 校验编码唯一性（排除自身）
        Long count = alarmConfigMapper.selectCount(
                new LambdaQueryWrapper<AlarmConfig>()
                        .eq(AlarmConfig::getRuleCode, dto.getRuleCode())
                        .ne(AlarmConfig::getId, id));
        if (count > 0) {
            throw new BusinessException("规则编码已存在: " + dto.getRuleCode());
        }

        AlarmConfig entity = converter.toEntity(dto);
        entity.setId(id);
        alarmConfigMapper.updateById(entity);
        log.info("更新报警配置: id={}", id);

        // 删除旧规则明细，重新插入
        alarmConfigMapper.softDeleteRulesByConfigId(id);
        saveRules(id, dto.getRules());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AlarmConfig config = alarmConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("报警配置不存在: id=" + id);
        }
        alarmConfigMapper.deleteById(id);
        alarmConfigMapper.softDeleteRulesByConfigId(id);
        log.info("删除报警配置: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new BusinessException("请选择要删除的记录");
        }
        for (Long id : ids) {
            delete(id);
        }
        log.info("批量删除报警配置: ids={}", ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id) {
        alarmConfigMapper.updateEnabled(id, 1);
        log.info("启用报警配置: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        alarmConfigMapper.updateEnabled(id, 0);
        log.info("禁用报警配置: id={}", id);
    }

    @Override
    public void exportExcel(AlarmConfigQueryDTO query, HttpServletResponse response) {
        query.setPage(1);
        query.setSize(9999);
        IPage<AlarmConfigListVO> result = page(query);

        try (ExcelWriter writer = ExcelUtil.getWriter(true)) {
            writer.addHeaderAlias("ruleName", "规则名称");
            writer.addHeaderAlias("ruleCode", "规则编码");
            writer.addHeaderAlias("unitName", "机组");
            writer.addHeaderAlias("specialtyName", "专业");
            writer.addHeaderAlias("systemName", "系统");
            writer.addHeaderAlias("measurePointName", "测点");
            writer.addHeaderAlias("alarmType", "报警类型");
            writer.addHeaderAlias("alarmLevel", "报警等级");
            writer.addHeaderAlias("enabled", "是否启用");
            writer.addHeaderAlias("description", "描述");
            writer.write(result.getRecords(), true);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String filename = URLEncoder.encode("报警配置.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + filename);
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            out.close();
            log.info("导出报警配置成功");
        } catch (IOException e) {
            log.error("导出 Excel 失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importFile(byte[] bytes, String filename) {
        try {
            ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes));
            reader.addHeaderAlias("规则名称", "ruleName");
            reader.addHeaderAlias("规则编码", "ruleCode");
            reader.addHeaderAlias("机组ID", "unitId");
            reader.addHeaderAlias("专业ID", "specialtyId");
            reader.addHeaderAlias("系统ID", "systemId");
            reader.addHeaderAlias("测点ID", "measurePointId");
            reader.addHeaderAlias("报警类型", "alarmType");
            reader.addHeaderAlias("报警等级", "alarmLevel");
            reader.addHeaderAlias("规则描述", "description");

            List<AlarmConfigSaveDTO> list = reader.readAll(AlarmConfigSaveDTO.class);
            reader.close();

            int count = 0;
            for (AlarmConfigSaveDTO dto : list) {
                try {
                    save(dto);
                    count++;
                } catch (Exception e) {
                    log.warn("导入跳过: code={}, msg={}", dto.getRuleCode(), e.getMessage());
                }
            }
            log.info("导入报警配置完成: 成功{}条", count);
        } catch (Exception e) {
            log.error("导入失败", e);
            throw new BusinessException("导入失败: " + e.getMessage());
        }
    }

    private void saveRules(Long configId, List<AlarmConfigSaveDTO.AlarmRuleDTO> ruleDTOs) {
        if (CollUtil.isEmpty(ruleDTOs)) {
            return;
        }
        List<AlarmRule> rules = converter.ruleDtoListToEntityList(ruleDTOs);
        List<AlarmRule> batchList = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            AlarmRule rule = rules.get(i);
            rule.setConfigId(configId);
            if (rule.getSortOrder() == null) {
                rule.setSortOrder(i);
            }
            batchList.add(rule);
            // 每 50 条批量插入一次
            if (batchList.size() >= 50) {
                for (AlarmRule r : batchList) {
                    alarmRuleMapper.insert(r);
                }
                batchList.clear();
            }
        }
        for (AlarmRule r : batchList) {
            alarmRuleMapper.insert(r);
        }
    }
}
