package com.rpgvtt.montador_de_rpg_backend.repository.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.EfeitoAtivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EfeitoAtivoRepository extends JpaRepository<EfeitoAtivo, Long> {

}