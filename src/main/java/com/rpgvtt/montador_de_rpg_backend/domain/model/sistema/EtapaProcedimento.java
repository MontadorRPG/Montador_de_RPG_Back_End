package com.rpgvtt.montador_de_rpg_backend.domain.model.sistema;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
        generator = "etapa_procedimento_seq"
    )
    @SequenceGenerator(
        name = "etapa_procedimento_seq", 
        sequenceName = "etapa_procedimento_sequence", 
        allocationSize = 1
    )
    @Column(name = "id_etapa")
    private Long idEtapa;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_procedimento")
    private Procedimento procedimento;


    @NotNull
    private Integer ordem;

    @NotNull
    private String nome;

    @Column(name = "tipo_etapa")
    private String tipoEtapa;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode parametrosEtapa;

    @NotNull
    private Boolean obrigatorio;

    public Boolean isObrigatorio() {return obrigatorio;}
}