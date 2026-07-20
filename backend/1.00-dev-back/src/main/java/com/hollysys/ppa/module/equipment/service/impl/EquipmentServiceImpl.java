package com.hollysys.ppa.module.equipment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hollysys.ppa.module.equipment.entity.DimMeasurePoint;
import com.hollysys.ppa.module.equipment.entity.DimSpecialty;
import com.hollysys.ppa.module.equipment.entity.DimSystem;
import com.hollysys.ppa.module.equipment.entity.DimUnit;
import com.hollysys.ppa.module.equipment.mapper.DimMeasurePointMapper;
import com.hollysys.ppa.module.equipment.mapper.DimSpecialtyMapper;
import com.hollysys.ppa.module.equipment.mapper.DimSystemMapper;
import com.hollysys.ppa.module.equipment.mapper.DimUnitMapper;
import com.hollysys.ppa.module.equipment.service.EquipmentService;
import com.hollysys.ppa.module.equipment.vo.EquipmentOptionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备管理 Service 实现
 *
 * @author PPA Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final DimUnitMapper dimUnitMapper;
    private final DimSpecialtyMapper dimSpecialtyMapper;
    private final DimSystemMapper dimSystemMapper;
    private final DimMeasurePointMapper dimMeasurePointMapper;

    @Override
    @Cacheable(value = "equipment:units", unless = "#result == null || #result.isEmpty()")
    public List<EquipmentOptionVO> getUnits() {
        log.info("查询机组下拉列表（DB）");
        List<DimUnit> list = dimUnitMapper.selectList(
                new LambdaQueryWrapper<DimUnit>()
                        .orderByAsc(DimUnit::getSortOrder));
        return list.stream()
                .map(u -> new EquipmentOptionVO(u.getId(), u.getUnitCode(), u.getUnitName()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "equipment:specialties", unless = "#result == null || #result.isEmpty()")
    public List<EquipmentOptionVO> getSpecialties() {
        log.info("查询专业下拉列表（DB）");
        List<DimSpecialty> list = dimSpecialtyMapper.selectList(
                new LambdaQueryWrapper<DimSpecialty>()
                        .orderByAsc(DimSpecialty::getSortOrder));
        return list.stream()
                .map(s -> new EquipmentOptionVO(s.getId(), s.getSpecialtyCode(), s.getSpecialtyName()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "equipment:systems", unless = "#result == null || #result.isEmpty()")
    public List<EquipmentOptionVO> getSystems() {
        log.info("查询系统下拉列表（DB）");
        List<DimSystem> list = dimSystemMapper.selectList(
                new LambdaQueryWrapper<DimSystem>()
                        .orderByAsc(DimSystem::getSortOrder));
        return list.stream()
                .map(s -> new EquipmentOptionVO(s.getId(), s.getSystemCode(), s.getSystemName(), s.getSpecialtyId()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "equipment:measurePoints", unless = "#result == null || #result.isEmpty()")
    public List<EquipmentOptionVO> getMeasurePoints() {
        log.info("查询测点下拉列表（DB）");
        List<DimMeasurePoint> list = dimMeasurePointMapper.selectList(
                new LambdaQueryWrapper<DimMeasurePoint>()
                        .orderByAsc(DimMeasurePoint::getSortOrder));
        return list.stream()
                .map(p -> new EquipmentOptionVO(
                        p.getId(),
                        p.getPointCode(),
                        p.getPointName() + (StrUtil.isNotBlank(p.getUnit()) ? " (" + p.getUnit() + ")" : ""),
                        p.getSystemId(),
                        p.getPointType()))
                .collect(Collectors.toList());
    }
}
