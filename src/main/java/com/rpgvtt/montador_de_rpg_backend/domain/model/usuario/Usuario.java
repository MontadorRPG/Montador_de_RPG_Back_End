package com.rpgvtt.montador_de_rpg_backend.domain.model.usuario;

// import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.MensagemLog;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Usuario {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_seq"
    )
    @SequenceGenerator(
            name = "user_seq",
            sequenceName = "user_sequence",
            allocationSize = 1
    )
    private Long id;

    private String senha;

    @NotBlank
    private String email;

    @NotBlank
    private String apelido;

    private boolean e_admin;

    // NOVOS CAMPOS PARA OAUTH2:
    private String provider;   // Salvará "GOOGLE" ou "DISCORD"
    private String providerId; // ID único que o Google/Discord gera para aquele usuário

    @CreationTimestamp
    @Column(name="criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name="atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "usuario")
    private List<Sistema> sistema;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "usuario")
    private List<Personagem> personagens;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "usuario")
    private List<MensagemLog> mensagens;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "usuario")
    private List<CampanhaUsuario> campanhas;
}
