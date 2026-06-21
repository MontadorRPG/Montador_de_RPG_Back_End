package com.rpgvtt.montador_de_rpg_backend.dto.personagem;

import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;

import java.util.List;

public record UsoItemDTO(
        String   idItem,
        String   nomeItem,
        Long     idInstanciaAlvo,
        boolean  restamUnidades,
        List<ResultadoEtapa> resultados
) {}