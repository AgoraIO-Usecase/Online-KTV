package com.agora.data.sync;

import androidx.annotation.NonNull;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/11
 */
public abstract class Converter {
    public abstract <T> T toObject(@NonNull String json, @NonNull Class<T> valueType);
}
