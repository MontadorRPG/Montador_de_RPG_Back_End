package com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador.contexto;

public record Alvo(String tipoEntidade, Object id) {
    @Override
    public String toString() {
        return tipoEntidade + ":" + id;
    }
}