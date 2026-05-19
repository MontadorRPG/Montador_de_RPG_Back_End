package com.rpgvtt.montador_de_rpg_backend.repository;

import com.rpgvtt.montador_de_rpg_backend.domain.model.EtapaProcedimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EtapaProcedimentoRepository extends JpaRepository<EtapaProcedimento, Long> {

}