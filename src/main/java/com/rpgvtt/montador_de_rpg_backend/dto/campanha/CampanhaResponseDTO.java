package com.rpgvtt.montador_de_rpg_backend.dto.campanha;

import java.time.LocalDateTime;

public record CampanhaResponseDTO(Long id,String nome,LocalDateTime criadaEm, Long sistemaId, String sistemaNome) {
    
}