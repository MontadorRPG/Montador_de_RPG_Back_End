package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import java.util.Map;

public record EfeitoManualDTO(
        Long idEfeitoAtivo,
        Long idInstancia,
        Map<String, Object> parametros,
        Integer expiraEm
) {}
