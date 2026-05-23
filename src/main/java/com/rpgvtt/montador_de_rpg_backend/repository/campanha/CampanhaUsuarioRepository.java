package com.rpgvtt.montador_de_rpg_backend.repository.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuarioKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampanhaUsuarioRepository extends JpaRepository<CampanhaUsuario, CampanhaUsuarioKey> {

}