package com.rpgvtt.montador_de_rpg_backend.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Evento_Sistema")
public class EventoSistema {
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
    @Column(name = "id_evento")
    private Long idEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Sistemas_id_sistema", referencedColumnName = "id_sistema")
    private Sistema sistema;

    private String nome;

    private String descricao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode payloadSchema;
}