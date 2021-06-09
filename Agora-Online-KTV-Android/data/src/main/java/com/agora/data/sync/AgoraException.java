package com.agora.data.sync;

import cn.leancloud.AVException;

/**
 * 异常
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/2
 */
public class AgoraException extends Exception {
    public static final int ERROR_OBJECT_NOT_FOUND = 100;
    public static final int ERROR_OTHER = 999;

    private int code;

    public AgoraException(int theCode, String theMessage) {
        super(theMessage);
        this.code = theCode;
    }

    public AgoraException(String message, Throwable cause) {
        super(message, cause);
        if (cause instanceof AVException) {
            if (((AVException) cause).getCode() == AVException.OBJECT_NOT_FOUND) {
                this.code = ERROR_OBJECT_NOT_FOUND;
            } else {
                this.code = ERROR_OTHER;
            }
        } else {
            this.code = ERROR_OTHER;
        }
    }

    public AgoraException(Throwable cause) {
        super(cause);
        if (cause instanceof AVException) {
            if (((AVException) cause).getCode() == AVException.OBJECT_NOT_FOUND) {
                this.code = ERROR_OBJECT_NOT_FOUND;
            } else {
                this.code = ERROR_OTHER;
            }
        } else {
            this.code = ERROR_OTHER;
        }
    }

    @Override
    public String toString() {
        return "code=" + code + ", " +
                "msg=" + getMessage();
    }
}
