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
public class Sistema {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sist_seq"
    )
    @SequenceGenerator(
            name = "sist_seq",
            sequenceName = "sist_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private String nome;

    private String descricao;

    @OneToMany(mappedBy = "sistema")
    private List<Campanha> campanhas;

    @OneToMany(mappedBy = "sistema")
    private List<TipoPersonagem> tiposPersonagens;

    @OneToMany(mappedBy = "sistema")
    private List<Personagem> personagens;
}
