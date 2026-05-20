package com.rpgvtt.montador_de_rpg_backend.domain.model.sistema;

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

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Procedimento {

    @Id
    @GeneratedValue (
        strategy = GenerationType.SEQUENCE,
        generator = "proced_seq"
    )
    @SequenceGenerator (
        name = "proced_seq",
        sequenceName = "proced_sequence",
        allocationSize = 1
    )
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "id_sistema")
    private Sistema sistema;

    private String descricao;
    private String tipo;

    @JdbcTypeCode (SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode confgsGeral;

    @OneToMany (cascade = CascadeType.ALL, mappedBy = "procedimento")
    private List<EtapasProcedimento> etapas = new ArrayList<>();
}
