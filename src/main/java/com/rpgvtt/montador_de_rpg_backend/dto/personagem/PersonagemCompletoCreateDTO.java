package com.rpgvtt.montador_de_rpg_backend.dto.personagem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record PersonagemCompletoCreateDTO(
        // Dados do usuário
        @NotNull(message = "O ID do usuário é obrigatório") Long usuarioId,

        // Dados da campanha
        Long campanhaId,

        // Dados da entidade sistema (para criar a instância)
        @NotNull(message = "O ID da entidade sistema é obrigatório") Long entidadeSistemaId,

        // Dados da instância
        @NotBlank(message = "O tipo é obrigatório") String tipo,
        @NotBlank(message = "O nome é obrigatório") String nome,
        String descricao,
        @NotNull(message = "Os atributos são obrigatórios") JsonNode atributosAtuais,
        JsonNode customizacoes,
        String urlImagem,

        // Dados específicos do personagem
        String historia,
        String aparencia,
        String notasJogador
) {}