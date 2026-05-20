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

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table (name = "etapas_procedimento")
public class EtapasProced {

    @Id 
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "etap_seq"
    )
    @SequenceGenerator (
        name = "etap_seq",
        sequenceName = "etap_sequence",
        allocationSize = 1
    )
    private Long id;

    @ManyToOne(fatch = FetchType.LAZY)
    @JoinColumn (name = "id_procedimento")
    private Procedimento procedimento;

    private String tipoEtapa;
    
    @JdbcTypeCode (SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode parametros;

    private boolean obrigatorio;


}
