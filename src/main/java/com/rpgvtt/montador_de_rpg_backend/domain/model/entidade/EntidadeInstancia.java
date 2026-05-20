package com.rpgvtt.montador_de_rpg_backend.domain.model.entidade;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;

import javax.annotation.processing.Generated;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;


@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table (name = "entidade_instancia")
public class EntidadeInstancia {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "inst_seq"
    )
    @SequenceGenerator (
        name = "inst_seq",
        sequenceName = "inst_sequence",
        allocationSize = 1
    )
    @Column(name = "id_instancia")
    private Long idInstancia;

    @ManyToOne(fatch = FetchType.LAZY)
    @JoinColumn (name = "id_campanha")
    private Campanha campanha;

    @ManyToOne(fatch = FetchType.LAZY)
    @JoinColumn (name = "id_entidade")
    private EntidadeSistema entidade;

    private String tipo;
    private String nome;
    private String descricao;

    @JdbcTypeCode (SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode AtributosAtuais;

    @JdbcTypeCode (SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode customizacoes;

    private LocalDateTime criadoEm;
    

}
