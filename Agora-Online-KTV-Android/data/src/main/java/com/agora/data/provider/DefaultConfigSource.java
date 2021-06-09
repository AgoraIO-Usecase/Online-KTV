package com.agora.data.provider;

/**
 * 默认配置文件
 *
 * @author chenhengfei(Aslanchen)
 */
public class DefaultConfigSource implements IConfigSource {

    @Override
    public String getUserTableName() {
        return "USER";
    }

    @Override
    public String getRoomTableName() {
        return "ROOM";
    }

    @Override
    public String getMemberTableName() {
        return "MEMBER";
    }

    @Override
    public String getActionTableName() {
        return "ACTION";
    }
}
