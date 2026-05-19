package com.rpgvtt.montador_de_rpg_backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode 
@Embeddable 
public class EntidadeRelacaoId implements Serializable {

    @Column(name = "id_entidade_pai")
    private Integer idEntidadePai;

    @Column(name = "id_entidade_filha")
    private Integer idEntidadeFilha;
}