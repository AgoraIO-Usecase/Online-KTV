package com.agora.data.provider;

import androidx.annotation.NonNull;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public interface IAgoraObject {

    <T> T toObject(@NonNull Class<T> valueType);

    String getId();
}
