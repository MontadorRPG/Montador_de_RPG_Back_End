package com.rpgvtt.montador_de_rpg_backend.repository.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampanhaRepository extends JpaRepository<Campanha, Long> {

    Optional<Campanha> findBySessoesId(Long idSessao);
}