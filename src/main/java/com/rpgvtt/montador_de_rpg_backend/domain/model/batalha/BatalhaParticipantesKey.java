package com.rpgvtt.montador_de_rpg_backend.domain.model.batalha;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class BatalhaParticipantesKey {

    @Column(name = "id_batalha")
    private Long idBatalha;

    @Column (name = "id_instancia")
    private Long idInstancia;
}
