package com.rpgvtt.montador_de_rpg_backend.domain.model.entidade;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.CenaParticipantes;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.EfeitoAtivo;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.HistoricoAcoes;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table (name = "entidade_instancia")
public class EntidadeInstancia {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "inst_seq"
    )
    @SequenceGenerator (
        name = "inst_seq",
        sequenceName = "inst_sequence",
        allocationSize = 1
    )
    @Column(name = "id_instancia")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn (name = "id_campanha")
    private Campanha campanha;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn (name = "id_entidade")
    private EntidadeSistema entidadeSistema;

    @NotNull
    private String tipo;

    @NotNull
    private String nome;

    private String descricao;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode atributosAtuais;

    // @NotNull nem sempre vai ter customizacoes
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode customizacoes;

    @CreationTimestamp
    @Column(name = "criada_em")
    private LocalDateTime criadaEm;

    @OneToMany(mappedBy = "entidadeInstancia")
    private List<EfeitoAtivo> efeitosAtivos;

    @OneToMany(mappedBy = "entidadeInstancia")
    private List<CenaParticipantes> cenas;

    @OneToMany(mappedBy = "entidadeInstancia")
    private List<HistoricoAcoes> acoes;
}
