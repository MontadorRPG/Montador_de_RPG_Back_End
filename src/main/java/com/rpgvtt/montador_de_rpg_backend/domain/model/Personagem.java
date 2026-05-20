package com.rpgvtt.montador_de_rpg_backend.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Personagem {
    @Id
    private Long id;

    private String nome;

    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_campanha")
    private Campanha campanha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipoPersonagem")
    private TipoPersonagem tipoPersonagem;

    @JdbcTypeCode(SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb")
    private JsonNode atributos;

    @OneToMany(mappedBy = "personagem")
    private List<HistoricoAcoes> historicoAcoes;

    @Column(columnDefinition = "jsonb")
    private String caracteristicas;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private EntidadeSistema entidade;
}
