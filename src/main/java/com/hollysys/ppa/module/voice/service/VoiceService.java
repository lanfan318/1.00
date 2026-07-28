package com.hollysys.ppa.module.voice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.module.voice.entity.VoiceRecord;
import com.hollysys.ppa.module.voice.vo.VoiceRecordVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音识别 Service
 */
public interface VoiceService {

    /** 上传音频 + STT 识别 + 指令执行 */
    VoiceRecordVO recognize(MultipartFile audio, Long sessionId, Long unitId,
                            String engineType, Boolean executeCommand);

    /** 纯文本指令执行（跳过 STT） */
    VoiceRecordVO executeTextCommand(String text, Long unitId, Boolean executeCommand);

    /** 分页查询语音记录 */
    Page<VoiceRecord> listRecords(Long sessionId, Integer page, Integer size);

    /** 查询单条记录 */
    VoiceRecordVO getRecord(Long id);
}
