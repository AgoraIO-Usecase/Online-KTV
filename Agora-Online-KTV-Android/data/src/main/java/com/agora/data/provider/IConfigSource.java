package com.agora.data.provider;

public interface IConfigSource {
    String getUserTableName();

    String getRoomTableName();

    String getMemberTableName();

    String getActionTableName();
}
