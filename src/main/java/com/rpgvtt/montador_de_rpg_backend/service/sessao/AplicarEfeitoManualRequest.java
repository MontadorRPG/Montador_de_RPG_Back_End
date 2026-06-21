package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record AplicarEfeitoManualRequest(
        @NotNull Long idInstancia,
        @NotNull Map<String, Object> parametros,
        Integer usos
//      List<Map<String, Object>> primitivosExpiracao
) {}