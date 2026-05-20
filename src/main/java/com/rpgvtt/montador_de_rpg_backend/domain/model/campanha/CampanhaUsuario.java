package com.rpgvtt.montador_de_rpg_backend.domain.model.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.PapeisUsuario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CampanhaUsuario {

    @EmbeddedId
    private CampanhaUsuarioKey id;

    private PapeisUsuario papel;

    private LocalDateTime entrouEm; 

}
