package com.rpgvtt.montador_de_rpg_backend.dto.campanha;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CampanhaCreateDTO(
    @NotBlank(message = "O nome da campanha não pode estar vazio") String nome,
    @NotNull(message = "O ID do sistema é obrigatório") Long sistemaId,
    @NotNull(message = "O ID do criador (Mestre) é obrigatório") Long criadorId
) {}