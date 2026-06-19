package com.rpgvtt.montador_de_rpg_backend.repository.entidade;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntidadeInstanciaRepository extends JpaRepository<EntidadeInstancia, Long> {

    // Optional<EntidadeInstancia> findBySessaoIdAndUsuarioId();

    // Optional<EntidadeInstancia> findByPersonagemId(Long personagemId);
}
