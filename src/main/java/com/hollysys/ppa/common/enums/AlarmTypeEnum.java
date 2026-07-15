package com.hollysys.ppa.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 报警类型枚举
 *
 * @author PPA Team
 */
@Getter
public enum AlarmTypeEnum {

    PREDICTION(1, "预测预警"),
    FAULT(2, "故障预警"),
    THRESHOLD(3, "阈值预警"),
    VIDEO(4, "视频预警"),
    RATE(5, "速率预警");

    @EnumValue
    @JsonValue
    private final int code;

    private final String label;

    AlarmTypeEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AlarmTypeEnum of(Integer code) {
        if (code == null) return null;
        for (AlarmTypeEnum e : values()) {
            if (e.code == code) return e;
        }
        return null;
    }
}
