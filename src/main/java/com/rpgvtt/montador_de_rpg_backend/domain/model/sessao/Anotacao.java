package com.rpgvtt.montador_de_rpg_backend.domain.model.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Anotacoes")
public class Anotacao {
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
    @Column(name = "id_anotacao")
    private Integer idAnotacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario_id_autor")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Campanhas_id_campanha")
    private Campanha campanha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Sessoes_id_sessao")
    private Sessao sessao;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "Cenas_id_cena")
    // private Cena cena;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "Entidade_Instancia_id_entidade")
    // private EntidadeInstancia entidadeInstancia;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "Entidade_Sistema_id_entidade")
    // private EntidadeSistema entidadeSistema;

    private String titulo;

    private String conteudo;

    private String categoria;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode anotacaoComplexa;

    @Column(name = "e_privado", nullable = false, columnDefinition = "boolean default true")
    private Boolean ePrivado = true;
}