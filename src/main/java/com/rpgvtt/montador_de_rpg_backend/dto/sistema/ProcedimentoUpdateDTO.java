package com.rpgvtt.montador_de_rpg_backend.dto.sistema;

import tools.jackson.databind.JsonNode;

public record ProcedimentoUpdateDTO(
        String nome,
        String descricao,
        String tipo,
        JsonNode configsGeral
) {}