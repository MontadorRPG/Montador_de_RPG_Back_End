package com.rpgvtt.montador_de_rpg_backend.domain.model.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Historico_Acoes")
public class HistoricoAcoes {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "histAc_seq"
    )
    @SequenceGenerator(
            name = "histAc_seq",
            sequenceName = "histAc_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sessao")
    private Sessao sessao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_campanha")
    private Campanha campanha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entidade")
    private EntidadeInstancia entidadeInstancia;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime momento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode resultado;

}
