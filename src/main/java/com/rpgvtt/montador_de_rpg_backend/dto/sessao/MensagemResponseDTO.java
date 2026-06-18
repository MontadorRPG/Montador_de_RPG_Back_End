package com.rpgvtt.montador_de_rpg_backend.dto.sessao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MensagemResponseDTO {
    private Long id;
    private Long sessaoId;
    private Long usuarioId;
    private Long campanhaId;
    private String conteudo;
    private LocalDateTime momento;
}
