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
@Table(name = "Entidade_Relacao")
public class EntidadeRelacao {
    @EmbeddedId 
    private EntidadeRelacaoId id = new EntidadeRelacaoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idEntidadePai") 
    @JoinColumn(name = "id_entidade_pai")
    private EntidadeSistema idEntidadePai;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idEntidadeFilha") 
    @JoinColumn(name = "id_entidade_filha")
    private EntidadeSistema idEntidadeFilha;

    private Integer quantidade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode customizacoes;

    private String origem;
}
