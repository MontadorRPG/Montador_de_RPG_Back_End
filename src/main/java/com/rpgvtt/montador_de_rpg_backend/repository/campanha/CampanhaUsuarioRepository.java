package com.rpgvtt.montador_de_rpg_backend.repository.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.PapeisUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuarioKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public interface CampanhaUsuarioRepository extends JpaRepository<CampanhaUsuario, CampanhaUsuarioKey> {

    Optional<CampanhaUsuario> findByUsuarioIdAndCampanhaIdAndPapel(Long idCampanha, Long idUsuario, PapeisUsuario papel);

    Optional<CampanhaUsuario> findByCampanhaIdAndUsuarioId(Long idCampanha, Long idUsuario);
}