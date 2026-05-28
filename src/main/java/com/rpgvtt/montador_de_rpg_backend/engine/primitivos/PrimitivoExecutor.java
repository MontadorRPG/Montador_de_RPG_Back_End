// domain/engine/primitivos/PrimitivoExecutor.java
package com.rpgvtt.montador_de_rpg_backend.engine.primitivos;

import com.rpgvtt.montador_de_rpg_backend.engine.utils.Contexto;
import com.fasterxml.jackson.databind.JsonNode;

public interface PrimitivoExecutor {
    void executar(JsonNode parametros, Contexto contexto, EstadoSessao estado);
}
