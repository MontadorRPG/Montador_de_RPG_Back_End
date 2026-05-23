package com.rpgvtt.montador_de_rpg_backend.repository.mecanica;

import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.Rolagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolagemRepository extends JpaRepository<Rolagem, Long> {

}