package com.hollysys.ppa.module.equipment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hollysys.ppa.module.equipment.entity.DimMeasurePoint;
import com.hollysys.ppa.module.equipment.entity.DimSpecialty;
import com.hollysys.ppa.module.equipment.entity.DimSystem;
import com.hollysys.ppa.module.equipment.entity.DimUnit;
import com.hollysys.ppa.module.equipment.mapper.DimMeasurePointMapper;
import com.hollysys.ppa.module.equipment.mapper.DimSpecialtyMapper;
import com.hollysys.ppa.module.equipment.mapper.DimSystemMapper;
import com.hollysys.ppa.module.equipment.mapper.DimUnitMapper;
import com.hollysys.ppa.module.equipment.service.impl.EquipmentServiceImpl;
import com.hollysys.ppa.module.equipment.vo.EquipmentOptionVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private DimUnitMapper dimUnitMapper;
    @Mock
    private DimSpecialtyMapper dimSpecialtyMapper;
    @Mock
    private DimSystemMapper dimSystemMapper;
    @Mock
    private DimMeasurePointMapper dimMeasurePointMapper;

    @InjectMocks
    private EquipmentServiceImpl equipmentService;

    @Test
    @DisplayName("获取机组列表")
    void testGetUnits() {
        DimUnit unit1 = new DimUnit();
        unit1.setId(1L);
        unit1.setUnitCode("10001");
        unit1.setUnitName("1号机组");

        when(dimUnitMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(unit1));

        List<EquipmentOptionVO> result = equipmentService.getUnits();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("10001");
        assertThat(result.get(0).getName()).isEqualTo("1号机组");
        verify(dimUnitMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取专业列表")
    void testGetSpecialties() {
        DimSpecialty sp = new DimSpecialty();
        sp.setId(1L);
        sp.setSpecialtyCode("BOILER");
        sp.setSpecialtyName("锅炉");

        when(dimSpecialtyMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(sp));

        List<EquipmentOptionVO> result = equipmentService.getSpecialties();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("BOILER");
        verify(dimSpecialtyMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取系统列表")
    void testGetSystems() {
        DimSystem sys = new DimSystem();
        sys.setId(1L);
        sys.setSystemCode("FGD");
        sys.setSystemName("脱硫系统");
        sys.setSpecialtyId(1L);

        when(dimSystemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(sys));

        List<EquipmentOptionVO> result = equipmentService.getSystems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("FGD");
        assertThat(result.get(0).getParentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("获取测点列表")
    void testGetMeasurePoints() {
        DimMeasurePoint mp = new DimMeasurePoint();
        mp.setId(1L);
        mp.setPointCode("10001:MALF114:IN");
        mp.setPointName("主蒸汽温度");
        mp.setUnit("℃");
        mp.setPointType("IN");
        mp.setSystemId(1L);

        when(dimMeasurePointMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(mp));

        List<EquipmentOptionVO> result = equipmentService.getMeasurePoints();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("10001:MALF114:IN");
        assertThat(result.get(0).getName()).contains("℃");
        assertThat(result.get(0).getExtra()).isEqualTo("IN");
    }
}
