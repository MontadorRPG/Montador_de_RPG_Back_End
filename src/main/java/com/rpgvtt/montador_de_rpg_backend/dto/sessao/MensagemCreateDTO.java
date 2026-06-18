package com.rpgvtt.montador_de_rpg_backend.dto.sessao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MensagemCreateDTO {
    private Long sessaoId;
    private Long usuarioId;
    private Long campanhaId;
    private String conteudo;
}
