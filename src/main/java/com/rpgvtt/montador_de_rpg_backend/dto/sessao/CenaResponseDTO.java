package com.rpgvtt.montador_de_rpg_backend.dto.sessao;
import tools.jackson.databind.JsonNode;

public record CenaResponseDTO(
    Long id,
    Long sessaoId,
    JsonNode mapaJson,
    String urlMapa,
    Integer ordem,
    String tipo,
    JsonNode estado
) {

    
}
