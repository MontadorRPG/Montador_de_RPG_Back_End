package com.rpgvtt.montador_de_rpg_backend.domain.model.entidade;

import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "Entidade_Sistema")
public class EntidadeSistema {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "entSist_seq"
    )
    @SequenceGenerator(
            name = "entSist_seq",
            sequenceName = "entSist_sequence",
            allocationSize = 1
    )
    @Column(name = "id_entidade")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;

    @NotNull
    private String tipo;

    @NotNull
    private String nome;

    private String descricao;

    @NotNull
    @JdbcTypeCode (SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode atributos;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode propriedades;

    @OneToMany(mappedBy = "entidade")
    private List<EntidadeSistema> entidadeSistemas;
}
