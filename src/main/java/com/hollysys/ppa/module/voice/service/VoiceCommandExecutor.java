package com.hollysys.ppa.module.voice.service;

import com.hollysys.ppa.common.R;
import com.hollysys.ppa.module.ai.service.AiEngineService;
import com.hollysys.ppa.module.alarm.realtime.service.AlarmRealtimeService;
import com.hollysys.ppa.module.alarm.stats.service.AlarmStatsService;
import com.hollysys.ppa.module.curve.service.CurveService;
import com.hollysys.ppa.module.realtime.service.RealtimeDataService;
import com.hollysys.ppa.module.voice.dto.VoiceCommandResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 语音指令执行器
 * 将转录文本匹配为系统操作并执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceCommandExecutor {

    private final RealtimeDataService realtimeDataService;
    private final AlarmStatsService alarmStatsService;
    private final AlarmRealtimeService alarmRealtimeService;
    private final AiEngineService aiEngineService;

    /**
     * 解析语音文本并执行对应操作
     */
    public VoiceCommandResult execute(String text, Long unitId) {
        String lower = text.toLowerCase().trim();

        // 匹配指令 → 执行操作
        if (matches(lower, "查询报警", "查看报警", "报警列表", "当前报警")) {
            return execQuery("query_alarm", "查询当前报警列表",
                    Map.of("action", "query_alarms", "unitId", unitId));
        }

        if (matches(lower, "确认报警", "确认")) {
            return execQuery("confirm_alarm", "确认报警",
                    Map.of("action", "confirm_alarm"));
        }

        if (matches(lower, "快照", "机组快照", "总览", "概况", "运行状态")) {
            return execQuery("snapshot", "查看机组快照",
                    Map.of("action", "snapshot", "unitId", unitId));
        }

        if (matches(lower, "健康", "健康评分", "设备状态")) {
            return execQuery("health", "查看设备健康评分",
                    Map.of("action", "health"));
        }

        if (matches(lower, "统计", "报表", "分析报告")) {
            return execQuery("stats", "查看统计报表",
                    Map.of("action", "stats"));
        }

        if (matches(lower, "曲线", "趋势", "工况")) {
            return execQuery("curve", "查看工况曲线",
                    Map.of("action", "curve"));
        }

        if (matches(lower, "诊断", "分析", "故障原因")) {
            return execQuery("diagnosis", "AI 诊断分析",
                    Map.of("action", "diagnosis"));
        }

        if (matches(lower, "预测", "预估")) {
            return execQuery("predict", "设备状态预测",
                    Map.of("action", "predict"));
        }

        // 未识别指令 → 作为对话
        return execQuery("chat", "AI 对话",
                Map.of("action", "chat", "message", text));
    }

    private VoiceCommandResult execQuery(String intent, String description, Map<String, Object> params) {
        log.info("语音指令: intent={}, desc={}", intent, description);
        VoiceCommandResult r = new VoiceCommandResult();
        r.setIntent(intent);
        r.setDescription(description);
        r.setParams(params);
        r.setSuccess(true);
        return r;
    }

    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
