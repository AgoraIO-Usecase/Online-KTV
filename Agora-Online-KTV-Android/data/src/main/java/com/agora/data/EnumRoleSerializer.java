package com.agora.data;

import com.agora.data.model.Member;
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
public class EnumRoleSerializer implements JsonSerializer<Member.Role>, JsonDeserializer<Member.Role> {
    @Override
    public Member.Role deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return Member.Role.parse(json.getAsInt());
    }

    @Override
    public JsonElement serialize(Member.Role src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getValue());
    }
}
