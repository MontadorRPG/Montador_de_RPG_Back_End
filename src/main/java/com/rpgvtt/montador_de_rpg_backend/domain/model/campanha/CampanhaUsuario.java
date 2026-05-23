package com.rpgvtt.montador_de_rpg_backend.domain.model.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.PapeisUsuario;
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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId(value = "idCampanha")
    @JoinColumn(name = "id_campanha")
    private Campanha campanha;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId(value = "idUsuario")
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

}
