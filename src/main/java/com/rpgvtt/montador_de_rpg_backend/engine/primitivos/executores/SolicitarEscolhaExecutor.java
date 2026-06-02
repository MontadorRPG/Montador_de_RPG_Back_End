package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.executores;

import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.EstadoSessao;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.PrimitivoExecutor;
// import com.rpgvtt.montador_de_rpg_backend.domain.engine.primitivos.Suspensao;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.Contexto;
import tools.jackson.databind.JsonNode;

// Primitivo: suspende a execução e apresenta opções ao jogador
// Parâmetros esperados:
//   "opcoes"       -> array de opções
public class SolicitarEscolhaExecutor implements PrimitivoExecutor {


    @Override
    public void executar(JsonNode parametros, Contexto contexto, EstadoSessao estado) {

        JsonNode opcoesNode     = parametros.get("opcoes");
        JsonNode guardarComoNode = parametros.get("guardar_como");

        if (opcoesNode == null || !opcoesNode.isArray() || guardarComoNode == null) {
            throw new IllegalArgumentException(
                "solicitar_escolha requer 'opcoes' (array) e 'guardar_como'"
            );
        }

        // estado.adicionarSuspensao(
        //     // new Suspensao(Suspensao.TipoSuspensao.ESCOLHA, guardarComoNode.asText(), opcoesNode)
        // );
    }
}