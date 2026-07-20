package com.hollysys.ppa.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 五大专业枚举
 *
 * @author PPA Team
 */
@Getter
public enum SpecialtyEnum {

    BOILER(1, "锅炉"),
    TURBINE(2, "汽机"),
    AUXILIARY(3, "辅网"),
    ELECTRICAL(4, "电气"),
    CHEMICAL(5, "化水");

    @EnumValue
    @JsonValue
    private final int code;

    private final String label;

    SpecialtyEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static SpecialtyEnum of(Integer code) {
        if (code == null) return null;
        for (SpecialtyEnum e : values()) {
            if (e.code == code) return e;
        }
        return null;
    }
}
