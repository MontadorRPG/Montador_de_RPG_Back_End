package com.rpgvtt.montador_de_rpg_backend.dto.entidade;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record EntidadeInstanciaCreateDTO(

    Long campanhaId,
    @NotNull(message = "O ID do sistema é obrigatorio") Long entidadeSistemaId,
    @NotBlank(message = "O Tipo é obrigatorio") String tipo,
    @NotBlank(message = "O nome é obrigatorio") String nome,
    String descricao,
    @NotNull(message = "Os atributos são obrigatorios") JsonNode atributosAtuais,
    JsonNode customizacoes,
    LocalDateTime criadaEm,
    String urlImagem

) {

}
