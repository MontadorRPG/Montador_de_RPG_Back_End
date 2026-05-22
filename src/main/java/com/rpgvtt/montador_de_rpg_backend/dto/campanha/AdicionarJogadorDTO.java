package com.rpgvtt.montador_de_rpg_backend.dto.campanha;

import jakarta.validation.constraints.NotNull;

public record AdicionarJogadorDTO(@NotNull(message = "O ID do usuário é obrigatório") Long usuarioId) {

}
