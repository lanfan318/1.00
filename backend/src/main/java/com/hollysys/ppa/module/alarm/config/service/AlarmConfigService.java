package com.hollysys.ppa.module.alarm.config.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hollysys.ppa.module.alarm.config.dto.AlarmConfigQueryDTO;
import com.hollysys.ppa.module.alarm.config.dto.AlarmConfigSaveDTO;
import com.hollysys.ppa.module.alarm.config.vo.AlarmConfigListVO;
import com.hollysys.ppa.module.alarm.config.vo.AlarmConfigVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 报警配置 Service 接口
 *
 * @author PPA Team
 */
public interface AlarmConfigService {

    /**
     * 分页查询报警配置
     */
    IPage<AlarmConfigListVO> page(AlarmConfigQueryDTO query);

    /**
     * 查询详情（含规则明细）
     */
    AlarmConfigVO getById(Long id);

    /**
     * 新增配置（含规则明细）
     */
    void save(AlarmConfigSaveDTO dto);

    /**
     * 编辑配置（含规则明细替换）
     */
    void update(Long id, AlarmConfigSaveDTO dto);

    /**
     * 删除配置（逻辑删除主表 + 所有子表）
     */
    void delete(Long id);

    /**
     * 批量删除
     */
    void batchDelete(List<Long> ids);

    /**
     * 启用
     */
    void enable(Long id);

    /**
     * 禁用
     */
    void disable(Long id);

    /**
     * 导出 Excel
     */
    void exportExcel(AlarmConfigQueryDTO query, HttpServletResponse response);

    /**
     * 导入 Excel / ZIP
     */
    void importFile(byte[] bytes, String filename);
}
