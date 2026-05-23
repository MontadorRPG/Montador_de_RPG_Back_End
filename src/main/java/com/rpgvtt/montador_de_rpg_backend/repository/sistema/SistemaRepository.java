package com.rpgvtt.montador_de_rpg_backend.repository.sistema;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SistemaRepository extends JpaRepository<Sistema, Long> {

}