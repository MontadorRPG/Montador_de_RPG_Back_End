// domain/engine/primitivos/PrimitivoExecutor.java
package com.rpgvtt.montador_de_rpg_backend.domain.engine.primitivos;

import com.rpgvtt.montador_de_rpg_backend.domain.engine.utils.Contexto;
import tools.jackson.databind.JsonNode;

public interface PrimitivoExecutor {
    void executar(JsonNode parametros, Contexto contexto, EstadoSessao estado);
}