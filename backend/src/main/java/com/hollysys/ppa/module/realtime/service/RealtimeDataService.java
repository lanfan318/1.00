package com.hollysys.ppa.module.realtime.service;

import com.hollysys.ppa.module.realtime.vo.RealtimeCurveVO;
import com.hollysys.ppa.module.realtime.vo.RealtimePointVO;
import com.hollysys.ppa.module.realtime.vo.SnapshotVO;

import java.util.List;

/**
 * 实时数据 Service
 */
public interface RealtimeDataService {

    /** 查询指定测点的最新值（卡片动态刷新） */
    List<RealtimePointVO> getLatestValues(Long unitId, List<Long> pointIds);

    /** 实时曲线数据（曲线图动态刷新） */
    RealtimeCurveVO getRealtimeCurve(Long unitId, String deviceCode, String curveType, Integer seconds);

    /** 机组快照（仪表盘总览） */
    SnapshotVO getSnapshot(Long unitId);
}
