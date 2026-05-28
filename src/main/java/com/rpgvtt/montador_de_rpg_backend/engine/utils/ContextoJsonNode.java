package com.rpgvtt.montador_de_rpg_backend.engine.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
// import tools.jackson.core.type.TypeReference;

public class ContextoJsonNode implements Contexto {

    private final JsonNode raiz;

    public ContextoJsonNode(JsonNode raiz) {
        this.raiz = raiz;
    }

    @Override
    public Optional<Object> get(String caminho) {
        
        String pointer = "/" + caminho.replace(".", "/");
        JsonNode node = raiz.at(pointer);
        if (node.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(converter(node));
    }

    private Object converter(JsonNode node) {
        if (node.isNumber()) return node.numberValue();
        if (node.isTextual()) return node.asText();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isArray()) {
            List<Object> lista = new ArrayList<>();
            for (JsonNode item : node) {
                lista.add(converter(item));
            }
            return lista;
        }
        if (node.isObject()) {
            return node;
        }
        return null;
    }
}
