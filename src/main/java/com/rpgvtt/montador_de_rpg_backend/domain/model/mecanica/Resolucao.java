package com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.TipoVantagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Resolucao")
public class Resolucao {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "res_seq"
    )
    @SequenceGenerator(
            name = "res_seq",
            sequenceName = "res_sistema_sequence",
            allocationSize = 1
    )
    @Column(name = "id_resolucao")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "vantagem")
    private TipoVantagem tipoVantagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Rolagem_id_rolagem")
    private Rolagem rolagem;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "Sistemas_id_sistema")
    private Sistema sistema;

    @Column(name = "alvo_tipo")
    private String alvoTipo;

    @Column(name = "alvo_referencia")
    private String alvoReferencia;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode parametros;
}
