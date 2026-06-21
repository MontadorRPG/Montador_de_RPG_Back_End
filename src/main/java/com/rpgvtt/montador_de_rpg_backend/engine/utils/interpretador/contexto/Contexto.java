package com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador.contexto;

import java.util.Optional;

public interface Contexto {
    Optional<Object> get(String caminho);
}
