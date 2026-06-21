package com.rpgvtt.montador_de_rpg_backend.config;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import tools.jackson.databind.json.JsonMapper;

public class Jackson3JsonFormatMapper implements FormatMapper {

    private final JsonMapper jsonMapper;

    public Jackson3JsonFormatMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        return jsonMapper.readValue(
                charSequence.toString(),
                jsonMapper.constructType(javaType.getJavaType())
        );
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        return jsonMapper.writeValueAsString(value);
    }
}