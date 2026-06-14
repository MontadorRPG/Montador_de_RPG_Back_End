package com.rpgvtt.montador_de_rpg_backend.repository.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusSessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    List<Sessao> findByStatusAndProcedimentoAtivoIsNotNull(StatusSessao status);

    Optional<Sessao> findAtivaByCampanhaId(Long idCampanha);

    int countByCampanhaId(Long idCampanha);
}