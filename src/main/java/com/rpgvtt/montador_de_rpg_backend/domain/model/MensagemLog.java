package com.rpgvtt.montador_de_rpg_backend.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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
        generator = "evento_sistema_seq"
    )
    @SequenceGenerator(
        name = "evento_sistema_seq", 
        sequenceName = "evento_sistema_sequence", 
        allocationSize = 1
    )
    @Column(name = "id_log")
    private Long idLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Sessoes_id_sessao")
    private Sessao sessao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuarios_id_usuario")
    private Usuario usuario;

    private String conteudo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime momento;
}