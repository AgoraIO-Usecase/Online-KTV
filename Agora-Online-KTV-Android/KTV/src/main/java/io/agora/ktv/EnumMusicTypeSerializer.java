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
public class EnumMusicTypeSerializer implements JsonSerializer<MemberMusicModel.SingType>, JsonDeserializer<MemberMusicModel.SingType> {
    @Override
    public MemberMusicModel.SingType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return MemberMusicModel.SingType.parse(json.getAsInt());
    }

    @Override
    public JsonElement serialize(MemberMusicModel.SingType src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.value);
    }
}
