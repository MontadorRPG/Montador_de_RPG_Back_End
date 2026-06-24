package com.rpgvtt.montador_de_rpg_backend.repository.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusSessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    List<Sessao> findByStatusAndProcedimentoAtivoIsNotNull(StatusSessao status);

    Optional<Sessao> findAtivaByCampanhaId(Long idCampanha);

    int countByCampanhaId(Long idCampanha);

    @Query("SELECT COUNT(s) FROM Sessao s WHERE s.campanha.id IN " +
           "(SELECT cu.id.idCampanha FROM CampanhaUsuario cu " +
           "WHERE cu.id.idUsuario = :usuarioId) " +
           "AND MONTH(s.dataInicio) = :mes AND YEAR(s.dataInicio) = :ano")
    long countByUsuarioIdAndMesAtual(@Param("usuarioId") Long usuarioId,
                                     @Param("mes") int mes,
                                     @Param("ano") int ano);
}