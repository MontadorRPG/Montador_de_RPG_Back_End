package com.rpgvtt.montador_de_rpg_backend.domain.model.sessao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;

// Tabela para guardar as instancias de entidade que estão em jogo atualmente. 
// Tabela de relação entre entidades instanciadas e cena.
// talvez mude para ser da sessão ao inves da cena.


@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table (name = "cena_participantes")
public class CenaParticipantes {

    @EmbeddedId
    private CenaParticipantesKey id;

    @JdbcTypeCode(SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode posicao;

}
