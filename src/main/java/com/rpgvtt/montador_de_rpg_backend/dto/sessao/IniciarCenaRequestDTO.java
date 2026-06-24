package com.rpgvtt.montador_de_rpg_backend.dto.sessao;

import java.util.List;

public record IniciarCenaRequestDTO(
    Long idSessao,
    List<Long> idsInstanciasJogadores,
    List<Long> idsInstanciasInimigos,
    List<List<Long>> lados,    // opcional
    String tipo,               // "COMBATE", "SOCIAL", etc.
    Integer ordem
) {}