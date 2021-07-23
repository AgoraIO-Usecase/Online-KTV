package io.agora.ktv;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import io.agora.ktv.bean.MemberMusicModel;

/**
 * 枚举转换规则
 *
 * @author chenhengfei(Aslanchen)
 */
public class EnumMusicUserStatusSerializer implements JsonSerializer<MemberMusicModel.UserStatus>, JsonDeserializer<MemberMusicModel.UserStatus> {
    @Override
    public MemberMusicModel.UserStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return MemberMusicModel.UserStatus.parse(json.getAsInt());
    }

    @Override
    public JsonElement serialize(MemberMusicModel.UserStatus src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.value);
    }
}
