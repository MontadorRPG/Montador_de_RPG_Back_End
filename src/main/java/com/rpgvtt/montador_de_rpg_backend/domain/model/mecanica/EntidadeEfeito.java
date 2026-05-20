package com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EventoSistema;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "id_evento")
    private EventoSistema evento;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "id_entidade")
    private EntidadeSistema entidade;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "id_resolucao")
    private Resolucao resolucao;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "id_rolagem")
    private Rolagem rolagem;

    private string processamento; // O quanto o sistema consegue processar essa efeito

    @JdbcTypeCode(SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode condicao;

    private boolean eReativo;

}
