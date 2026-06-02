package com.rpgvtt.montador_de_rpg_backend.domain.model.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

// Tabela para guardar as instancias de entidade que estão em jogo atualmente. 
// Tabela de relação entre entidades instanciadas e cena.
// talvez mude para ser da sessão ao inves da cena.

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cena_participantes")
public class CenaParticipantes {

    @EmbeddedId
    private CenaParticipantesKey id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode posicao;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId(value = "idCena")
    @JoinColumn(name = "id_cena")
    private Cena cena;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId(value = "idInstancia")
    @JoinColumn(name = "id_instancia")
    private EntidadeInstancia entidadeInstancia;

}
