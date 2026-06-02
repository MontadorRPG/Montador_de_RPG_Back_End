package com.rpgvtt.montador_de_rpg_backend.domain.model.sistema;

// import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.EntidadeEfeito;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

// import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Evento_Sistema")
public class EventoSistema {
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
    @Column(name = "id_evento")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;

    @NotNull
    private String nome;

    private String descricao;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode payloadSchema;

//     @OneToMany(mappedBy = "evento")
//     private List<EntidadeEfeito> efeitos;
}