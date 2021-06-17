package com.agora.data.provider;

import androidx.annotation.NonNull;

import com.agora.data.sync.IAgoraObject;
import com.agora.data.sync.SyncManager;

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
        return SyncManager.getConverter().toObject(avObject.toJSONObject().toJSONString(), valueType);
    }

    @Override
    public String getId() {
        return avObject.getObjectId();
    }

    public Object get(String key) {
        Object object = avObject.get(key);
        if (object instanceof AVObject) {
            object = new AgoraObject((AVObject) object);
        }
        return object;
    }
}
