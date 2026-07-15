package com.hollysys.ppa.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 *
 * @param <T> 数据类型
 * @author PPA Team
 */
@Data
@Schema(description = "统一响应")
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码，0 表示成功", example = "0")
    private int code;

    @Schema(description = "提示消息", example = "success")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "时间戳", example = "1700000000000")
    private long timestamp;

    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.code = 0;
        r.msg = "success";
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 0;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> R<T> ok(String msg, T data) {
        R<T> r = new R<>();
        r.code = 0;
        r.msg = msg;
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.code = -1;
        r.msg = msg;
        return r;
    }
}
