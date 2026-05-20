package com.rpgvtt.montador_de_rpg_backend.domain.model;

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
public class Usuario {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_seq"
    )
    @SequenceGenerator(
            name = "user_seq",
            sequenceName = "user_sequence",
            allocationSize = 1
    )
    private Long id;

    private String senha;
    private String email;
    private String apelido;
    private boolean e_admin;

    private LocalDateTime criado_em;
    private LocalDateTime atualizado_em;

    @OneToMany(mappedBy = "usuario")
    private List<Sistema> sistema;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "usuario")
    private List<Personagem> personagens;
}
