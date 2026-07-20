package com.hollysys.ppa.module.curve.service;

import com.hollysys.ppa.module.curve.entity.HealthScore;
import com.hollysys.ppa.module.curve.vo.CurveDataVO;

/**
 * 工况曲线 Service
 *
 * @author PPA Team
 */
public interface CurveService {

    CurveDataVO getCurve(Long unitId, String deviceCode, String type, String range);

    HealthScore getHealth(Long unitId, String deviceCode);
}
