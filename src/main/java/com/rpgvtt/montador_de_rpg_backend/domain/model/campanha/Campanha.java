package com.rpgvtt.montador_de_rpg_backend.domain.model.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Campanha {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "camp_seq"
    )
    @SequenceGenerator(
            name = "camp_seq",
            sequenceName = "camp_sequence",
            allocationSize = 1
    )
    private Long id;

    private String nome;

    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "id_mestre")
//     private Usuario mestre;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "campanha")
    private List<Sessao> sessoes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "campanha")
    private List<Personagem> personagens;
}
