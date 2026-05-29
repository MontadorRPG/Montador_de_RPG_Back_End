package com.rpgvtt.montador_de_rpg_backend.domain.model.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Cena {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "cena_seq"
    )
    @SequenceGenerator(
            name = "cena_seq",
            sequenceName = "cena_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sessao")
    private Sessao sessao;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode mapa;

    @NotNull
    private Integer ordem;

    @OneToMany(mappedBy = "cena")
    private List<CenaParticipantes> participantes;

//     @JdbcTypeCode(SqlTypes.JSON)
//     @Column(columnDefinition = "jsonb")
//     private JsonNode estado;

}
