// UsuarioUpdateDTO.java
package com.rpgvtt.montador_de_rpg_backend.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateDTO(
        @NotBlank(message = "O apelido não pode estar vazio")
        @Size(min = 3, max = 50, message = "O apelido deve ter entre 3 e 50 caracteres")
        String apelido,

        String urlImagem
) {}