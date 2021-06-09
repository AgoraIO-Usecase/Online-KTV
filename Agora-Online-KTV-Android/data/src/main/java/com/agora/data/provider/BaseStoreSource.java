package com.agora.data.provider;

import androidx.annotation.NonNull;

public abstract class BaseStoreSource implements IStoreSource {
    protected IConfigSource mIConfigSource;

    public BaseStoreSource(@NonNull IConfigSource mIConfigSource) {
        this.mIConfigSource = mIConfigSource;
    }
}
