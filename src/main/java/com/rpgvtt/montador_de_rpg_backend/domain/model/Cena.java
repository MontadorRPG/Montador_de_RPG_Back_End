package com.rpgvtt.montador_de_rpg_backend.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Cena {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "cena_seq"
    )
    @SequenceGenerator(
            name = "cena_seq",
            sequenceName = "cena_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sessao")
    private Sessao sessao;

    @JdbcTypeCode (SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode mapa;

//     @JdbcTypeCode(SqlTypes.JSONB)
//     @Column(columnDefinition = "jsonb")
//     private JsonNode estado;

}
