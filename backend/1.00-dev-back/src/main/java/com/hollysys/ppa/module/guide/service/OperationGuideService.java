package com.hollysys.ppa.module.guide.service;

import com.hollysys.ppa.module.guide.entity.OperationGuide;

/**
 * 操作指导 Service
 *
 * @author PPA Team
 */
public interface OperationGuideService {

    /**
     * 获取报警的操作指导（优先查本地，无则转发 KG 服务）
     */
    OperationGuide getByAlarmId(Long alarmId);

    /**
     * 填写运行意见
     */
    void addComment(Long alarmId, String comment, String operator);
}
