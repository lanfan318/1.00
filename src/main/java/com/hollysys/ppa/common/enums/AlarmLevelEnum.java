package com.hollysys.ppa.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 报警等级枚举
 *
 * @author PPA Team
 */
@Getter
public enum AlarmLevelEnum {

    LEVEL_1(1, "一级"),
    LEVEL_2(2, "二级"),
    SMART_WARNING(3, "智能预警");

    @EnumValue
    @JsonValue
    private final int code;

    private final String label;

    AlarmLevelEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AlarmLevelEnum of(Integer code) {
        if (code == null) return null;
        for (AlarmLevelEnum e : values()) {
            if (e.code == code) return e;
        }
        return null;
    }
}
