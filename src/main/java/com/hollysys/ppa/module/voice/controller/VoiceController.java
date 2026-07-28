package com.hollysys.ppa.module.voice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.voice.entity.VoiceRecord;
import com.hollysys.ppa.module.voice.service.VoiceService;
import com.hollysys.ppa.module.voice.vo.VoiceRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 语音识别 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Tag(name = "语音识别", description = "语音上传识别 / 文字指令执行 / 语音记录查询")
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping("/recognize")
    @Operation(summary = "上传音频识别 + 指令执行",
            description = "上传 WAV/MP3 音频文件，进行语音转文字，可选执行语音指令")
    public R<VoiceRecordVO> recognize(
            @Parameter(description = "音频文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "会话ID") @RequestParam(required = false) Long sessionId,
            @Parameter(description = "机组ID") @RequestParam(required = false) Long unitId,
            @Parameter(description = "引擎类型") @RequestParam(required = false) String engine,
            @Parameter(description = "是否执行指令") @RequestParam(defaultValue = "true") Boolean execute) {
        log.info("语音识别: file={}, engine={}, execute={}", file.getOriginalFilename(), engine, execute);
        return R.ok(voiceService.recognize(file, sessionId, unitId, engine, execute));
    }

    @PostMapping("/command")
    @Operation(summary = "文字指令执行",
            description = "直接输入文本执行语音指令（跳过 STT），适合文本聊天或调试")
    public R<VoiceRecordVO> textCommand(@RequestBody Map<String, Object> body) {
        String text = (String) body.get("text");
        Long unitId = body.get("unitId") != null ? ((Number) body.get("unitId")).longValue() : null;
        Boolean execute = body.get("execute") != null ? (Boolean) body.get("execute") : true;
        log.info("文字指令: text=\"{}\"", text);
        return R.ok(voiceService.executeTextCommand(text, unitId, execute));
    }

    @GetMapping("/records")
    @Operation(summary = "查询语音识别记录")
    public R<Page<VoiceRecord>> listRecords(
            @Parameter(description = "会话ID") @RequestParam(required = false) Long sessionId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer size) {
        return R.ok(voiceService.listRecords(sessionId, page, size));
    }

    @GetMapping("/records/{id}")
    @Operation(summary = "查询单条语音记录详情")
    public R<VoiceRecordVO> getRecord(@PathVariable Long id) {
        return R.ok(voiceService.getRecord(id));
    }
}
