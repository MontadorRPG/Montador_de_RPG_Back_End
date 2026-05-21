package com.rpgvtt.montador_de_rpg_backend.domain.model.sistema;

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
@Table(name = "Etapas_Procedimento")
public class EtapaProcedimento {
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
    @Column(name = "id_etapa")
    private Long idEtapa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Procedimentos_id_procedimento")
    private Procedimento procedimento;

    private Integer ordem;

    private String nome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode parametros_etapa;

    private Boolean obrigatorio;
}