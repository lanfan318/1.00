package com.hollysys.ppa.module.guide.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hollysys.ppa.common.BusinessException;
import com.hollysys.ppa.infra.KgClient;
import com.hollysys.ppa.module.guide.entity.OperationGuide;
import com.hollysys.ppa.module.guide.mapper.OperationGuideMapper;
import com.hollysys.ppa.module.guide.service.OperationGuideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 操作指导 Service 实现（KG 转发 + 落库）
 *
 * @author PPA Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationGuideServiceImpl implements OperationGuideService {

    private final OperationGuideMapper guideMapper;
    private final KgClient kgClient;

    @Override
    public OperationGuide getByAlarmId(Long alarmId) {
        // 先查本地缓存
        OperationGuide local = guideMapper.selectOne(
                new LambdaQueryWrapper<OperationGuide>()
                        .eq(OperationGuide::getAlarmId, alarmId)
                        .orderByDesc(OperationGuide::getCreatedAt)
                        .last("LIMIT 1"));
        if (local != null) {
            return local;
        }

        // 转发 KG 服务并落库
        log.info("本地无操作指导，转发 KG 服务: alarmId={}", alarmId);
        try {
            String kgResult = kgClient.getOperationGuide(alarmId);
            JSONObject kgJson = JSON.parseObject(kgResult);
            JSONObject data = kgJson.getJSONObject("data");
            if (data != null) {
                OperationGuide guide = new OperationGuide();
                guide.setAlarmId(alarmId);
                guide.setTitle(data.getString("title"));
                guide.setContent(data.getString("content"));
                guide.setDiagnosis(data.getString("diagnosis"));
                guide.setSuggestion(data.getString("suggestion"));
                guide.setRiskLevel(data.getString("riskLevel"));
                guide.setSource(1); // KG
                guideMapper.insert(guide);
                log.info("KG 操作指导落库: alarmId={}, guideId={}", alarmId, guide.getId());
                return guide;
            }
        } catch (Exception e) {
            log.error("KG 服务获取操作指导失败: alarmId={}", alarmId, e);
        }
        throw new BusinessException("未找到操作指导: alarmId=" + alarmId);
    }

    @Override
    public void addComment(Long alarmId, String comment, String operator) {
        OperationGuide guide = guideMapper.selectOne(
                new LambdaQueryWrapper<OperationGuide>()
                        .eq(OperationGuide::getAlarmId, alarmId)
                        .orderByDesc(OperationGuide::getCreatedAt)
                        .last("LIMIT 1"));
        if (guide == null) {
            throw new BusinessException("未找到操作指导: alarmId=" + alarmId);
        }
        guide.setComment(comment);
        guide.setCommentBy(operator);
        guideMapper.updateById(guide);
        log.info("填写运行意见: alarmId={}, operator={}", alarmId, operator);
    }
}
