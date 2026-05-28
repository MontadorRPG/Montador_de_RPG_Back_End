package com.rpgvtt.montador_de_rpg_backend.domain.model.sistema;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;

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

    @ManyToOne (optional = false, fetch = FetchType.LAZY)
    @JoinColumn (name = "id_sistema")
    private Sistema sistema;

    @NotNull
    private String nome;

    @SuppressWarnings("unused")
    private String descricao;
    
    @SuppressWarnings("unused")
    private String tipo;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode confgsGeral;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "procedimento")
    private List<EtapaProcedimento> etapas;
}
