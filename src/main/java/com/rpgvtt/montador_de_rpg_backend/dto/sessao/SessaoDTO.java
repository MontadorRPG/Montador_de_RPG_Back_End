package com.rpgvtt.montador_de_rpg_backend.dto.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusSessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;

import java.time.LocalDateTime;

public record SessaoDTO(Long idSessao, StatusSessao status, LocalDateTime dataInicio, Long idCampanha) {
}
