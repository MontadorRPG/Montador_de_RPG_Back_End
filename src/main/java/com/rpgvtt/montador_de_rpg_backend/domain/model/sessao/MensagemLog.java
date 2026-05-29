package com.rpgvtt.montador_de_rpg_backend.domain.model.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Mensagens_Log")
public class MensagemLog {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE, 
        generator = "mensagem_seq"
    )
    @SequenceGenerator(
        name = "mensagem_seq",
        sequenceName = "evento_sistema_sequence", 
        allocationSize = 1
    )
    @Column(name = "id_log")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "Sessoes_id_sessao")
    private Sessao sessao;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuarios_id_usuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Campanhas_id_campanha")
    private Campanha campanha;

    @NotNull
    private String conteudo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime momento;
}