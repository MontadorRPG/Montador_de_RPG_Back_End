package com.rpgvtt.montador_de_rpg_backend.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Entidade_Sistema")
public class EntidadeSistema {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "ent_seq"
    )
    @SequenceGenerator(
            name = "ent_seq",
            sequenceName = "ent_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;

    private String nome;

    private String descricao;

    @OneToMany(mappedBy = "entidade")
    private List<EntidadeSistema> entidadeSistemas;
}
