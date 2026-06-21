package com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador.contexto;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        return Optional.ofNullable(converter(node));
    }

    private Object converter(JsonNode node) {
        if (node.isNumber()) return node.numberValue();
        if (node.isString()) return node.asString();
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
