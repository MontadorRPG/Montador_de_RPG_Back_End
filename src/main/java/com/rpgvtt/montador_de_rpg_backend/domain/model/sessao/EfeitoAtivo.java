package com.rpgvtt.montador_de_rpg_backend.domain.model.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.EntidadeEfeito;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table (name = "efeito_ativo")
public class EfeitoAtivo {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "efAt_seq"
    )
    @SequenceGenerator(
            name = "efAt_seq",
            sequenceName = "efAt_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_instancia")
    private EntidadeInstancia entidadeInstancia;

//     @ManyToOne(optional = false, fetch = FetchType.LAZY)
//     @JoinColumn(name = "id_efeito")
//     private EntidadeEfeito entidadeEfeito;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sessao")
    private Sessao sessao;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode parametros;

    @NotNull
    private Integer expiraEm;

    @Column(name = "usos_restantes")
    private Integer usosRestantes;

}
