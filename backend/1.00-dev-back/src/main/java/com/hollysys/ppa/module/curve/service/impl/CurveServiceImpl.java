package com.hollysys.ppa.module.curve.service.impl;

import com.hollysys.ppa.common.BusinessException;
import com.hollysys.ppa.module.curve.entity.EquipmentCurve;
import com.hollysys.ppa.module.curve.entity.HealthScore;
import com.hollysys.ppa.module.curve.mapper.EquipmentCurveMapper;
import com.hollysys.ppa.module.curve.service.CurveService;
import com.hollysys.ppa.module.curve.vo.CurveDataVO;
import com.hollysys.ppa.module.equipment.entity.DimUnit;
import com.hollysys.ppa.module.equipment.mapper.DimUnitMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工况曲线 Service 实现
 *
 * @author PPA Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurveServiceImpl implements CurveService {

    private final EquipmentCurveMapper curveMapper;
    private final DimUnitMapper dimUnitMapper;

    private LocalDateTime resolveStart(String range) {
        return switch (range) {
            case "1h" -> LocalDateTime.now().minusHours(1);
            case "6h" -> LocalDateTime.now().minusHours(6);
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "7d" -> LocalDateTime.now().minusDays(7);
            default -> LocalDateTime.now().minusHours(24);
        };
    }

    @Override
    public CurveDataVO getCurve(Long unitId, String deviceCode, String type, String range) {
        log.info("查询工况曲线: unitId={}, device={}, type={}, range={}", unitId, deviceCode, type, range);
        LocalDateTime start = resolveStart(range);
        LocalDateTime end = LocalDateTime.now();

        List<EquipmentCurve> curves = curveMapper.selectCurve(unitId, deviceCode, type, start, end);

        CurveDataVO vo = new CurveDataVO();
        vo.setUnitId(unitId);
        vo.setDeviceCode(deviceCode);
        vo.setCurveType(type);
        vo.setPoints(curves.stream()
                .map(c -> new CurveDataVO.CurvePoint(c.getSampleTime(), c.getSampleValue(), c.getQualityFlag()))
                .collect(Collectors.toList()));
        return vo;
    }

    @Override
    public HealthScore getHealth(Long unitId, String deviceCode) {
        log.info("查询健康评分: unitId={}, device={}", unitId, deviceCode);
        HealthScore score = curveMapper.selectLatestHealth(unitId, deviceCode);
        if (score == null) {
            throw new BusinessException("未找到健康评分数据");
        }
        return score;
    }
}
