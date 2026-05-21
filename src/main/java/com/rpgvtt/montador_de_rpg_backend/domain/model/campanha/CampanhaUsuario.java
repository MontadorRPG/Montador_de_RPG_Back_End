package com.rpgvtt.montador_de_rpg_backend.domain.model.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.model.PapeisUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
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
public class CampanhaUsuario {

    @EmbeddedId
    private CampanhaUsuarioKey id;

    private PapeisUsuario papel;

    @CreationTimestamp
    @Column(name = "entrou_em")
    private LocalDateTime entrouEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_campanha")
    private Campanha campanha;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

}
