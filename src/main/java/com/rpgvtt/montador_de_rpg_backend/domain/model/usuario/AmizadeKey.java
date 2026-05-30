package com.rpgvtt.montador_de_rpg_backend.domain.model.usuario;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AmizadeKey implements Serializable {
    private Long idRemetente;
    private Long idDestinatario;
}