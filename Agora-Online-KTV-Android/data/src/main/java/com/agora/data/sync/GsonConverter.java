package com.agora.data.sync;

import androidx.annotation.NonNull;

import com.agora.data.EnumActionSerializer;
import com.agora.data.EnumRoleSerializer2;
import com.agora.data.model.Action;
import com.agora.data.model.AgoraMember;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/11
 */
public class GsonConverter extends Converter {

    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(AgoraMember.Role.class, new EnumRoleSerializer2())
            .registerTypeAdapter(Action.ACTION.class, new EnumActionSerializer())
            .create();

    @Override
    public <T> T toObject(@NonNull String json, @NonNull Class<T> valueType) {
        return mGson.fromJson(json, valueType);
    }
}
