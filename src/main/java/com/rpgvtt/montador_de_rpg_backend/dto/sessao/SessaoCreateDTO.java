package com.rpgvtt.montador_de_rpg_backend.dto.sessao;

import jakarta.validation.constraints.NotNull;

/**
 * Record que representa os dados necessários para abrir uma nova sessão de jogo.
 */
public record SessaoCreateDTO(@NotNull(message = "O ID da campanha é obrigatório para iniciar uma sessão.")Long campanhaId) {
    
}
