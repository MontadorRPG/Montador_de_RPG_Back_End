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
public class EfeitoAtivo {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "efAt_seq"
    )
    @SequenceGenerator(
            name = "efAt_seq",
            sequenceName = "efAt_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_personagem")
    private Personagem personagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_efeito")
    private EntidadeEfeito entidadeEfeito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sessao")
    private Sessao sessao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode parametros;

    private Integer expiraEm;

    @Column(name = "usos_restantes")
    private Integer usosRestantes;

}
