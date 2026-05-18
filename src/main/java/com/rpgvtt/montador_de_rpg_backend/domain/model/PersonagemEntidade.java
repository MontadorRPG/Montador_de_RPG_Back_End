package com.rpgvtt.montador_de_rpg_backend.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "personagem_entidade")
public class PersonagemEntidade {

    @EmbeddedId
    PersonagemEntidadeKey id;

    @ManyToOne
    @MapsId
    @JoinColumn(name = "id_personagem")
    private Personagem personagem;

    @ManyToOne
    @MapsId
    @JoinColumn(name = "id_entidade")
    private EntidadeSistema entidade;

    private String origem;

}
