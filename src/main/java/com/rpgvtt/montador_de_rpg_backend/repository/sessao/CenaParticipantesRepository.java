package com.rpgvtt.montador_de_rpg_backend.repository.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Cena;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.CenaParticipantes;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.CenaParticipantesKey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CenaParticipantesRepository extends JpaRepository<CenaParticipantes, CenaParticipantesKey> {

    List<CenaParticipantes> findByCena(Cena cena);
}
