package com.rpgvtt.montador_de_rpg_backend.repository.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Cena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CenaRepository extends JpaRepository<Cena, Long> {

}