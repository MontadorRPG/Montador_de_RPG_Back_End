package com.rpgvtt.montador_de_rpg_backend.repository;

import com.rpgvtt.montador_de_rpg_backend.domain.model.Anotacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnotacaoRepository extends JpaRepository<Anotacao, Integer> {

}