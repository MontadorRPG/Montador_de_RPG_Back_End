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
@Table(name = "Tipos_Persoangem")
public class TipoPersonagem {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "tiposPer_seq"
    )
    @SequenceGenerator(
            name = "tiposPer_seq",
            sequenceName = "tiposPer_sequence",
            allocationSize = 1
    )
    private Long id;

    private String nome;

    @OneToMany(mappedBy = "tipoPersonagem")
    private List<Personagem> personagens;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "atributos_base", columnDefinition = "jsonb")
    private JsonNode atributosBase;
}
