package com.rpgvtt.montador_de_rpg_backend.domain.model.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusCampanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;

// import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Campanha {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "camp_seq"
    )
    @SequenceGenerator(
            name = "camp_seq",
            sequenceName = "camp_sequence",
            allocationSize = 1
    )
    private Long id;

    @NotNull
    private String nome;

    private String descricao;

    private String urlImagem; // Icone de campanha

    @NotNull
    private StatusCampanha Status;


    @CreationTimestamp
    @Column(name = "criada_em")
    private LocalDateTime criadaEm;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "campanha")
    private List<Sessao> sessoes;

//     @OneToMany(cascade = CascadeType.ALL, mappedBy = "campanha")
//     private List<Personagem> personagens;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "campanha")
    private List<CampanhaUsuario> usuarios;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "campanha")
    private List<EntidadeInstancia> entidadesInstanciadas;
}
