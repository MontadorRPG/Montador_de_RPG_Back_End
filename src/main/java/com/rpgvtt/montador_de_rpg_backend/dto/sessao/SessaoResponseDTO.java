package com.rpgvtt.montador_de_rpg_backend.dto.sessao;

import java.time.LocalDateTime;

/**
 * Record que devolve os dados simplificados da sessão para o frontend.
 */
public record SessaoResponseDTO(Long id, Long campanhaId, LocalDateTime dataInicio, LocalDateTime dataFim) {
    
}