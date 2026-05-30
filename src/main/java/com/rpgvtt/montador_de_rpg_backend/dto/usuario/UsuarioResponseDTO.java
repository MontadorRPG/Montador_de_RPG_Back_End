// UsuarioResponseDTO.java
package com.rpgvtt.montador_de_rpg_backend.dto.usuario;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id,
        String email,
        String apelido,
        String urlImagem,
        boolean eAdmin,
        LocalDateTime criadoEm
) {}