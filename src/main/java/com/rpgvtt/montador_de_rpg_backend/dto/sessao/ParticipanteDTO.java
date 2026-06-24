package com.rpgvtt.montador_de_rpg_backend.dto.sessao;

import tools.jackson.databind.JsonNode;

public record ParticipanteDTO(
    Long idInstancia,
    String nome,
    String tipo,
    JsonNode posicao,
    JsonNode atributosAtuais
) {}
