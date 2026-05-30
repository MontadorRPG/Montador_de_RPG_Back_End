
package com.rpgvtt.montador_de_rpg_backend.dto.usuario;

import jakarta.validation.constraints.NotNull;

public record AmizadeSolicitarDTO(
        @NotNull(message = "O ID do remetente é obrigatório") Long remetenteId,
        @NotNull(message = "O ID do destinatário é obrigatório") Long destinatarioId
) {}