package com.rpgvtt.montador_de_rpg_backend.domain.model.usuario;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusAmizade;
import jakarta.persistence.*;
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
public class Amizade {

    @EmbeddedId  
    private AmizadeKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRemetente")
    @JoinColumn(name = "id_remetente")
    private Usuario remetente;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idDestinatario")
    @JoinColumn(name = "id_destinatario")
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAmizade status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime criadaEm;

    private LocalDateTime aceitoEm;
}