package io.agora.ktv;

import androidx.annotation.NonNull;

import com.agora.data.model.AgoraMember;
import com.agora.data.sync.EnumRoleSerializer;
import com.agora.data.sync.GsonConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.agora.ktv.bean.MemberMusicModel;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/20
 */
public class MyGsonConverter extends GsonConverter {
    private final Gson mGson = new GsonBuilder()
            .registerTypeAdapter(AgoraMember.Role.class, new EnumRoleSerializer())
            .registerTypeAdapter(MemberMusicModel.SingType.class, new EnumMusicTypeSerializer())
            .registerTypeAdapter(MemberMusicModel.UserStatus.class, new EnumMusicUserStatusSerializer())
            .create();

    @Override
    public <T> T toObject(@NonNull String json, @NonNull Class<T> valueType) {
        return mGson.fromJson(json, valueType);
    }
}
