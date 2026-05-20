package com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;

// Tabela de relação entre entidade efeito e primitivos


@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table (name = "efeitos_primitivos")
public class EfeitosPrimitivos {

    @EmbeddedId
    private EfeitosPrimitivosKey id;

    private int ordem;

    @JdbcTypeCode(SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode parametros;

}
