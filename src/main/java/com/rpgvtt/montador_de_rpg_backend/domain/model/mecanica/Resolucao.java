package com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Resolucao")
public class Resolucao {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE, 
        generator = "evento_sistema_seq"
    )
    @SequenceGenerator(
        name = "evento_sistema_seq", 
        sequenceName = "evento_sistema_sequence", 
        allocationSize = 1
    )
    @Column(name = "id_etapa")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "Sistemas_id_sistema")
    private Sistema sistema;

    @NotNull
    private String nome;

    @NotNull
    private String tipo;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode parametros;
}