package com.rpgvtt.montador_de_rpg_backend.dto.personagem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PersonagemCreateDTO(
        @NotNull(message = "O ID do usuário é obrigatório") Long usuarioId,
        Long campanhaId,
        @NotNull(message = "O ID da instância é obrigatório") Long instanciaId,
        @NotBlank(message = "O nome é obrigatório") String nome,
        String historia,
        String aparencia,
        String urlImagem,
        String notasJogador
) {}