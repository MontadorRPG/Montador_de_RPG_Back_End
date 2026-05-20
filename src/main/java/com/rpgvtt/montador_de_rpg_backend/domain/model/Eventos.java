package com.rpgvtt.montador_de_rpg_backend.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Eventos {

    @Id
    @GeneratedValue (
        strategy = GenerationType.SEQUENCE,
        generator = "envet_seq"
    )
    @SequenceGenerator (
        name = "envet_seq",
        sequenceName = "envet_sequence",
        allocationSize = 1
    )
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;

    private String nome;
    private String descricao;

    @JdbcTypeCode(SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode payload;



}
