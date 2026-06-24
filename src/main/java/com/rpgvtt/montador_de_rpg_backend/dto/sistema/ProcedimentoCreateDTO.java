package com.rpgvtt.montador_de_rpg_backend.dto.sistema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record ProcedimentoCreateDTO(
        @NotNull(message = "O ID do sistema é obrigatório") Long sistemaId,
        @NotBlank(message = "O nome é obrigatório") String nome,
        String descricao,
        String tipo,
        @NotNull(message = "As configurações gerais são obrigatórias") JsonNode configsGeral
) {}