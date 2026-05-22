package com.rpgvtt.montador_de_rpg_backend.domain.model.sistema;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
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
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Sistema {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sist_seq"
    )
    @SequenceGenerator(
            name = "sist_seq",
            sequenceName = "sist_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_criador")
    private Usuario usuario;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "id_sistema_pai")
    private Sistema sistemaPai;

    @NotNull
    private String nome;

    private String descricao;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode schemaAtributos;

    @NotNull
    @JdbcTypeCode (SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode schemaEntidades;

    @NotNull
    private Integer versaoSchemas;

    @NotNull
    private boolean eOficial;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "sistema")
    private List<Campanha> campanhas;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistema")
    private List<EntidadeInstancia> personagens;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistema")
    private List<Procedimento> procedimento;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistema")
    private List<EventoSistema> eventos;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistemaPai")
    private List<Sistema> sistemasFilhos;


}
