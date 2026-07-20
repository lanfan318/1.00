package com.hollysys.ppa.module.equipment.service;

import com.hollysys.ppa.module.equipment.vo.EquipmentOptionVO;

import java.util.List;

/**
 * 设备管理 Service 接口
 *
 * @author PPA Team
 */
public interface EquipmentService {

    /**
     * 获取机组下拉列表
     */
    List<EquipmentOptionVO> getUnits();

    /**
     * 获取专业下拉列表
     */
    List<EquipmentOptionVO> getSpecialties();

    /**
     * 获取系统下拉列表
     */
    List<EquipmentOptionVO> getSystems();

    /**
     * 获取测点下拉列表
     */
    List<EquipmentOptionVO> getMeasurePoints();
}
