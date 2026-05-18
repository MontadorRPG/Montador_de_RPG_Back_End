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
@Table(name = "Entidade_Efeito")
public class EntidadeEfeito {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "entEf_seq"
    )
    @SequenceGenerator(
            name = "entEf_seq",
            sequenceName = "entEf_sequence",
            allocationSize = 1
    )
    private Long id;

    @Many
}
