package com.rpgvtt.montador_de_rpg_backend.domain.model.sistema;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_criador")
    private Usuario usuario;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "id_sistema_pai ")
    private Sistema sistemaPai;

    private String nome;

    private String descricao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode schemaAtributos;

    @JdbcTypeCode (SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode schemaEntidades;

    private int versaoSchemas;

    private boolean eOficial;
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "sistema")
    private List<Campanha> campanhas = new ArrayList<>();

    // @OneToMany(mappedBy = "sistema")
    // private List<TipoPersonagem> tiposPersonagens = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistema")
    private List<Personagem> personagens = new ArrayList<>();

    @OneToMany (cascade = CascadeType.ALL, mappedBy = "sistema")
    private List<Procedimento> procedimento = new ArrayList<>();

    @OneToMany (cascade = CascadeType.ALL, mappedBy = "sistema")
    private List<EventoSistema> eventos = new ArrayList<>();

    @OneToMany (cascade = CascadeType.ALL, mappedBy = "sistemaPai")
    private List<Sistema> sistemasFilhos = new ArrayList<>();


}
