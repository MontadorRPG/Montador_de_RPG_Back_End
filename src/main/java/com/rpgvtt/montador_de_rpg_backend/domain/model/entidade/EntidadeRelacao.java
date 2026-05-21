package com.rpgvtt.montador_de_rpg_backend.domain.model.entidade;

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
@Table(name = "Entidade_Relacao")
public class EntidadeRelacao {
    @EmbeddedId 
    private EntidadeRelacaoKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idEntidadePai") 
    @JoinColumn(name = "id_entidade_pai")
    private EntidadeSistema idEntidadePai;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idEntidadeFilha") 
    @JoinColumn(name = "id_entidade_filha")
    private EntidadeSistema idEntidadeFilha;

    @NotNull
    private Integer quantidade;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode customizacoes;

    private String origem;
}
