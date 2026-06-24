package com.rpgvtt.montador_de_rpg_backend.repository.mecanica;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.EntidadeProcedimento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntidadeProcedimentoRepository extends JpaRepository<EntidadeProcedimento, Long> {

    Optional<EntidadeProcedimento> findByEntidadeInstancia(EntidadeInstancia entidadeId);
    // Optional<EntidadeProcedimento> findByEntidadeSistemaAndProcessamentoNot();
}
