package com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Resolucao")
public class Resolucao {
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
    @Column(name = "id_resolucao")
    private Long idResolucao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Rolagem_id_rolagem")
    private Rolagem rolagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Sistemas_id_sistema")
    private Sistema sistema;

    @Column(name = "alvo_referencia")
    private String alvoReferencia;

    @JdbcTypeCode(SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode parametros;
}
