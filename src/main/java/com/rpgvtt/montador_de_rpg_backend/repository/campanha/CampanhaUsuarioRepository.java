package com.rpgvtt.montador_de_rpg_backend.repository.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.PapeisUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuarioKey;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaParticipanteResponseDTO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampanhaUsuarioRepository extends JpaRepository<CampanhaUsuario, CampanhaUsuarioKey> {

    Optional<CampanhaUsuario> findByCampanhaIdAndUsuarioIdAndPapel(Long idCampanha, Long idUsuario, PapeisUsuario papel);

    List<CampanhaUsuario> findByCampanhaId(Long campanhaId);

    Optional<CampanhaUsuario> findByCampanhaIdAndUsuarioId(Long idCampanha, Long idUsuario);
}