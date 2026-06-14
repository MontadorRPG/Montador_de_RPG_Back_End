package com.rpgvtt.montador_de_rpg_backend.dto.sessao;

public record EntradaSessaoDTO(Long idSessao, Long idUusario, boolean isMestre, Long idInstancia) {
}
