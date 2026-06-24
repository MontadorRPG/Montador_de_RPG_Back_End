package com.rpgvtt.montador_de_rpg_backend.dto.sessao;

public record CenaCombateDTO(
    Long id,
    String status,
    int rodadaAtual
) {}