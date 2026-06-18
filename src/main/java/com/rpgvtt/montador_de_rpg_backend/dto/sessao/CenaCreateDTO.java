package com.rpgvtt.montador_de_rpg_backend.dto.sessao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CenaCreateDTO {
    private Long sessaoId;
    private JsonNode mapaJson;
    private String urlMapa;
    private Integer ordem;
}
