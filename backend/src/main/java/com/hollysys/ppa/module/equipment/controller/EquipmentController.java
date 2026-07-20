package com.hollysys.ppa.module.equipment.controller;

import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.equipment.service.EquipmentService;
import com.hollysys.ppa.module.equipment.vo.EquipmentOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备管理 Controller — 下拉列表接口
 *
 * @author PPA Team
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "机组 / 专业 / 系统 / 测点下拉列表，Redis 缓存")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping("/units")
    @Operation(summary = "机组下拉列表")
    public R<List<EquipmentOptionVO>> getUnits() {
        log.info("获取机组下拉列表");
        return R.ok(equipmentService.getUnits());
    }

    @GetMapping("/specialties")
    @Operation(summary = "专业下拉列表")
    public R<List<EquipmentOptionVO>> getSpecialties() {
        log.info("获取专业下拉列表");
        return R.ok(equipmentService.getSpecialties());
    }

    @GetMapping("/systems")
    @Operation(summary = "系统下拉列表")
    public R<List<EquipmentOptionVO>> getSystems() {
        log.info("获取系统下拉列表");
        return R.ok(equipmentService.getSystems());
    }

    @GetMapping("/measure-points")
    @Operation(summary = "测点下拉列表")
    public R<List<EquipmentOptionVO>> getMeasurePoints() {
        log.info("获取测点下拉列表");
        return R.ok(equipmentService.getMeasurePoints());
    }
}
