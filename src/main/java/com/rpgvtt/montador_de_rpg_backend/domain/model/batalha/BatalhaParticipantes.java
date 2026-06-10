package com.rpgvtt.montador_de_rpg_backend.domain.model.batalha;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
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
@Table(name = "batalha_participantes")
public class BatalhaParticipantes {

    @EmbeddedId
    private BatalhaParticipantesKey id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode posicao;

    private Integer lado;

    @Column(name = "ordem_iniciativa")
    private Integer ordemIniciativa;

    @Column(name = "resultado_iniciativa")
    private Integer resultadoIniciativa;

    private boolean isAtivo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId(value = "idBatalha")
    @JoinColumn(name = "id_batalha")
    private Batalha batalha;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId(value = "idInstancia")
    @JoinColumn(name = "id_instancia")
    private EntidadeInstancia entidadeInstancia;
}
