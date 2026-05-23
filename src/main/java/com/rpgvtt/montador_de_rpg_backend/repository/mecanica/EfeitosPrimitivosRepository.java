package com.rpgvtt.montador_de_rpg_backend.repository.mecanica;

import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.EfeitosPrimitivos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EfeitosPrimitivosRepository extends JpaRepository<EfeitosPrimitivos, Long> {
}
