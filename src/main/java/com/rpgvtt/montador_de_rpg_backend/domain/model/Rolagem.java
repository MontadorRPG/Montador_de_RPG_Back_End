package com.rpgvtt.montador_de_rpg_backend.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity

@Table(name = "Rolagem")
public class Rolagem {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "evento_sistema_seq"
    )
    @SequenceGenerator(
            name = "evento_sistema_seq",
            sequenceName = "evento_sistema_sequence",
            allocationSize = 1
    )
    @Column(name = "id_rolagem")
    private Long idRolagem;

    private String dado;

    private Integer quantidade;

    private Boolean explosao;

    @Enumerated(EnumType.STRING)
    @Column(name = "vantagem_desvantagem", length = 11, nullable = true) 
    private TipoVantagem vantagemDesvantagem;

    @JdbcTypeCode(SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode valorAgregado;
}
