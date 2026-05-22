package com.rpgvtt.montador_de_rpg_backend.dto.campanha;

import java.time.LocalDateTime;

public record CampanhaParticipanteResponseDTO(Long campanhaId, Long usuarioId, String papel, LocalDateTime entrouEm) {

}