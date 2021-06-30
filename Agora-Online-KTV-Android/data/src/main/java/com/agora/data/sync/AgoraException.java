package com.agora.data.sync;

import cn.leancloud.LCException;

/**
 * 异常
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/2
 */
public class AgoraException extends Exception {
    public static final int ERROR_DEFAULT = 1;

    public static final int ERROR_OBJECT_NOT_FOUND = 100;
    public static final int ERROR_LEANCLOULD_DEFAULT = ERROR_OBJECT_NOT_FOUND + 1;//leancloud
    public static final int ERROR_LEANCLOULD_OVER_COUNT = ERROR_LEANCLOULD_DEFAULT + 1;//超过开发版应用的功能限制，请升级到商用版，如有疑问请联系我们

    private int code;

    public AgoraException(int theCode, String theMessage) {
        super(theMessage);
        this.code = theCode;
    }

    public AgoraException(String message, Throwable cause) {
        super(message, cause);
        if (cause instanceof LCException) {
            if (((LCException) cause).getCode() == LCException.OBJECT_NOT_FOUND) {
                this.code = ERROR_OBJECT_NOT_FOUND;
            } else {
                this.code = ERROR_DEFAULT;
            }
        } else {
            this.code = ERROR_DEFAULT;
        }
    }

    public AgoraException(Throwable cause) {
        super(cause);
        if (cause instanceof LCException) {
            if (((LCException) cause).getCode() == LCException.OBJECT_NOT_FOUND) {
                this.code = ERROR_OBJECT_NOT_FOUND;
            } else {
                this.code = ERROR_DEFAULT;
            }
        } else {
            this.code = ERROR_DEFAULT;
        }
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "code=" + code + ", " +
                "msg=" + getMessage();
    }
}
