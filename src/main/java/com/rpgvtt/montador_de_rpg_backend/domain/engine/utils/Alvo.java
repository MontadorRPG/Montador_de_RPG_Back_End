package com.rpgvtt.montador_de_rpg_backend.domain.engine.utils;

public record Alvo(String tipoEntidade, Object id) {
    @Override
    public String toString() {
        return tipoEntidade + ":" + id;
    }
}