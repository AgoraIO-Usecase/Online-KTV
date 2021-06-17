package com.agora.data.sync;

import com.agora.data.model.AgoraMember;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * 枚举转换规则
 *
 * @author chenhengfei(Aslanchen)
 */
public class EnumRoleSerializer implements JsonSerializer<AgoraMember.Role>, JsonDeserializer<AgoraMember.Role> {
    @Override
    public AgoraMember.Role deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return AgoraMember.Role.parse(json.getAsInt());
    }

    @Override
    public JsonElement serialize(AgoraMember.Role src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getValue());
    }
}
