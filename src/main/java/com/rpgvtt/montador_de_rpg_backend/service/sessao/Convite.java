package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import java.time.LocalDateTime;

public record Convite(Long idUsuario, String token, LocalDateTime expira) {
}
