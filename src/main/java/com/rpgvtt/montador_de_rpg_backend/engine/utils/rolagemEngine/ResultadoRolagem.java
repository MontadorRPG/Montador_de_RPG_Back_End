package com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine;

import java.util.List;

// DTO de resultado
public record ResultadoRolagem(
        String dado,
        List<Integer> rolos,   // cada rolo individual (para exibir ao jogador)
        int total
) {
    public boolean isCritico(int faces) {
        return rolos.size() == 1 && rolos.get(0) == faces;
    }
    public boolean isFalhaCritica() {
        return rolos.size() == 1 && rolos.get(0) == 1;
    }
}
