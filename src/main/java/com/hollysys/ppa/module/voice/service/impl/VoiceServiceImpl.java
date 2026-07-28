package com.hollysys.ppa.module.voice.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.module.voice.dto.VoiceCommandResult;
import com.hollysys.ppa.module.voice.engine.SttEngine;
import com.hollysys.ppa.module.voice.entity.VoiceRecord;
import com.hollysys.ppa.module.voice.mapper.VoiceRecordMapper;
import com.hollysys.ppa.module.voice.service.VoiceCommandExecutor;
import com.hollysys.ppa.module.voice.service.VoiceService;
import com.hollysys.ppa.module.voice.vo.VoiceRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 语音识别 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceServiceImpl implements VoiceService {

    private final List<SttEngine> sttEngines;
    private final VoiceRecordMapper recordMapper;
    private final VoiceCommandExecutor commandExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceRecordVO recognize(MultipartFile audio, Long sessionId, Long unitId,
                                    String engineType, Boolean executeCommand) {
        long start = System.currentTimeMillis();

        // 选择 STT 引擎
        SttEngine engine = selectEngine(engineType);

        SttEngine.SttResult sttResult;
        try {
            byte[] audioData = audio.getBytes();
            sttResult = engine.recognize(audioData, "wav");
        } catch (Exception e) {
            log.error("音频读取失败", e);
            VoiceRecord record = saveRecord(sessionId, unitId, "", 0, engine.getName(),
                    0, "failed", "音频读取失败: " + e.getMessage(), null);
            return toVO(record, null);
        }

        // 执行语音指令
        VoiceCommandResult cmdResult = null;
        if (Boolean.TRUE.equals(executeCommand) && sttResult.text() != null && !sttResult.text().isBlank()) {
            cmdResult = commandExecutor.execute(sttResult.text(), unitId);
        }

        // 保存记录
        VoiceRecord record = saveRecord(sessionId, unitId, sttResult.text(), sttResult.confidence(),
                engine.getName(), System.currentTimeMillis() - start,
                "success", null, cmdResult);

        log.info("语音识别完成: text=\"{}\", intent={}, cost={}ms",
                sttResult.text(), cmdResult != null ? cmdResult.getIntent() : "none",
                System.currentTimeMillis() - start);

        return toVO(record, cmdResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceRecordVO executeTextCommand(String text, Long unitId, Boolean executeCommand) {
        VoiceCommandResult cmdResult = null;
        if (Boolean.TRUE.equals(executeCommand)) {
            cmdResult = commandExecutor.execute(text, unitId);
        }

        VoiceRecord record = saveRecord(null, unitId, text, 1.0,
                "text_input", 0L, "success", null, cmdResult);

        return toVO(record, cmdResult);
    }

    @Override
    public Page<VoiceRecord> listRecords(Long sessionId, Integer page, Integer size) {
        Page<VoiceRecord> p = new Page<>(page != null ? page : 1, size != null ? size : 20);
        LambdaQueryWrapper<VoiceRecord> qw = new LambdaQueryWrapper<>();
        if (sessionId != null) {
            qw.eq(VoiceRecord::getSessionId, sessionId);
        }
        qw.orderByDesc(VoiceRecord::getCreatedAt);
        return recordMapper.selectPage(p, qw);
    }

    @Override
    public VoiceRecordVO getRecord(Long id) {
        VoiceRecord record = recordMapper.selectById(id);
        if (record == null) return null;
        VoiceCommandResult cmd = null;
        if (record.getIntentParams() != null) {
            try {
                cmd = JSON.parseObject(record.getIntentParams(), VoiceCommandResult.class);
            } catch (Exception ignored) {}
        }
        return toVO(record, cmd);
    }

    // ─── 内部方法 ───

    private SttEngine selectEngine(String engineType) {
        if (engineType != null && !engineType.isBlank()) {
            for (SttEngine e : sttEngines) {
                if (e.getName().equals(engineType) && e.isAvailable()) {
                    return e;
                }
            }
        }
        // 默认返回第一个可用的
        return sttEngines.stream().filter(SttEngine::isAvailable).findFirst()
                .orElseThrow(() -> new RuntimeException("没有可用的 STT 引擎"));
    }

    private VoiceRecord saveRecord(Long sessionId, Long unitId, String text, double confidence,
                                    String engineType, long costMs, String status, String errorMsg,
                                    VoiceCommandResult cmd) {
        VoiceRecord r = new VoiceRecord();
        r.setSessionId(sessionId);
        r.setUnitId(unitId);
        r.setTranscriptText(text);
        r.setConfidence(confidence);
        r.setEngineType(engineType);
        r.setEngineVersion("v1.0");
        r.setCostMs(costMs);
        r.setStatus("success".equals(status) ? 1 : 0);
        r.setErrorMsg(errorMsg);
        if (cmd != null) {
            r.setIntent(cmd.getIntent());
            r.setIntentParams(JSON.toJSONString(cmd));
        } else {
            r.setIntent(text != null && !text.isBlank() ? "chat" : "unknown");
        }
        recordMapper.insert(r);
        return r;
    }

    private VoiceRecordVO toVO(VoiceRecord record, VoiceCommandResult cmd) {
        VoiceRecordVO vo = new VoiceRecordVO();
        vo.setId(record.getId());
        vo.setSessionId(record.getSessionId());
        vo.setTranscriptText(record.getTranscriptText());
        vo.setConfidence(record.getConfidence());
        vo.setIntent(record.getIntent());
        vo.setIntentResult(cmd);
        vo.setEngineType(record.getEngineType());
        vo.setAudioDurationMs(record.getAudioDurationMs());
        vo.setCostMs(record.getCostMs());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
