package com.agora.data.provider;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import cn.leancloud.AVObject;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public class AgoraObject implements IAgoraObject {

    private final AVObject avObject;

    public AgoraObject(AVObject avObject) {
        this.avObject = avObject;
    }

    @Override
    public <T> T toObject(@NonNull Class<T> valueType) {
        return new Gson().fromJson(avObject.toJSONObject().toJSONString(), valueType);
    }

    @Override
    public String getId() {
        return avObject.getObjectId();
    }
}
