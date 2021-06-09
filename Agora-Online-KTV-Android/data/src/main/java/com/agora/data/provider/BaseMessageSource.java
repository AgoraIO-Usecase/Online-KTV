package com.agora.data.provider;

import androidx.annotation.NonNull;

public abstract class BaseMessageSource implements IMessageSource {
    protected IRoomProxy iRoomProxy;
    protected IConfigSource mIConfigSource;

    public BaseMessageSource(@NonNull IRoomProxy iRoomProxy, @NonNull IConfigSource mIConfigSource) {
        this.iRoomProxy = iRoomProxy;
        this.mIConfigSource = mIConfigSource;
    }
}
