package com.rpgvtt.montador_de_rpg_backend.repository.personagem;

import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonagemRepository extends JpaRepository<Personagem, Long> {

    List<Personagem> findBySessaoId(Long aLong);
}