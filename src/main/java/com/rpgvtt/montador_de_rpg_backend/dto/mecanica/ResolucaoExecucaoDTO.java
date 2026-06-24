package com.rpgvtt.montador_de_rpg_backend.dto.mecanica;

import tools.jackson.databind.JsonNode;

public record ResolucaoExecucaoDTO(
        Object roll,
        int targetValue,
        boolean success,
        String motivo,
        JsonNode parametros
) {}
