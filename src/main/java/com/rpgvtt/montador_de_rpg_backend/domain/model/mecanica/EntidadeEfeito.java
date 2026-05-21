package com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EventoSistema;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "Entidade_Efeito")
public class EntidadeEfeito {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "entEf_seq"
    )
    @SequenceGenerator(
            name = "entEf_seq",
            sequenceName = "entEf_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private EventoSistema evento;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entidade")
    private EntidadeSistema entidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_resolucao")
    private Resolucao resolucao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rolagem")
    private Rolagem rolagem;

    @NotNull
    private String processamento; // O quanto o sistema consegue processar essa efeito

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode condicao;

    @NotNull
    private boolean eReativo;

    @OneToMany(mappedBy = "efeito")
    private List<EfeitosPrimitivos> primitivos;

}
