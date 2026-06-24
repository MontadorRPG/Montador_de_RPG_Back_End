package com.rpgvtt.montador_de_rpg_backend.dto.importacao;

import tools.jackson.databind.JsonNode;

public record DefinicaoDTO(String alias, String tipo, JsonNode dados) {}
