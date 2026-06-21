package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import java.util.Map;

public record DescansoDTO(
        String              tipo,
        Map<String, Object> restaurado
) {}
