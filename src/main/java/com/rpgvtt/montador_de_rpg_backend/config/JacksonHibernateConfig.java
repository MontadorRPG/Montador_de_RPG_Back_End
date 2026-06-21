package com.rpgvtt.montador_de_rpg_backend.config;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonHibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer jsonFormatMapperCustomizer(JsonMapper jsonMapper) {
        return properties -> properties.put(
                "hibernate.type.json_format_mapper",
                new Jackson3JsonFormatMapper(jsonMapper)
        );
    }
}