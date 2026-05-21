package com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica;

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
@Table(name = "Primitivos")
public class Primitivo {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE, 
        generator = "primitivo_seq"
    )
    @SequenceGenerator(
        name = "primitivo_seq",
        sequenceName = "primitivo_sequence",
        allocationSize = 1
    )
    @Column(name = "id_primitivo")
    private Long id;

    @NotNull
    private String nome;

    private String descricao;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode parametro_schemas;

    @OneToMany(mappedBy = "primitivo")
    private List<EfeitosPrimitivos> efeitos;
}