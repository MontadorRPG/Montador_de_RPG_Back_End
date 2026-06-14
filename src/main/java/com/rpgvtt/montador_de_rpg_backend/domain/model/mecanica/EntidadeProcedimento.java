package com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table (name = "entidade_procedimento")
public class EntidadeProcedimento {
    
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE, 
        generator = "entidade_procedimento_seq"
    )
    @SequenceGenerator(
        name = "entidade_procedimento_seq",
        sequenceName = "entidade_procedimento_sequence", 
        allocationSize = 1
    )
    @Column(name = "id_entidade_procedimento")
    private Long id;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn (name = "id_sistema")
    private Sistema sistema;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn (name = "id_entidade")
    private EntidadeSistema entidadeSistema;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_procedimento")
    private Procedimento procedimento;
    
    // Se é automatico (sistema consegue realizar tudo), parcial (Tem que pedir algo ao usuário) ou narrativo (o sistema não consegue fazer)
    private String processamento;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode condicao;

    @NotNull
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean eReativo = false;

    private Integer ordem;



}
