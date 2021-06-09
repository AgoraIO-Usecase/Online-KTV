package com.agora.data;

/**
 * 基础错误类
 *
 * @author Aslan
 * @date 2019/9/23
 */
public class BaseError extends Exception {

    private int code;

    public BaseError(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
