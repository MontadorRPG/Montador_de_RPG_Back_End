package com.rpgvtt.montador_de_rpg_backend.repository.entidade;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntidadeSistemaRepository extends JpaRepository<EntidadeSistema, Long> {
    List<EntidadeSistema> findBySistemaId(Long sistemaId);

    List<EntidadeSistema> findBySistemaIdAndTipo(Long sistemaId, String tipo);
}