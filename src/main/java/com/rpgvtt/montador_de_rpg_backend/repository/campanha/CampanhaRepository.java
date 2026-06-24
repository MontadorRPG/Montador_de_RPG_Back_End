package com.rpgvtt.montador_de_rpg_backend.repository.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampanhaRepository extends JpaRepository<Campanha, Long> {

       Optional<Campanha> findBySessoesId(Long idSessao);

       @Query("SELECT COUNT(c) FROM Campanha c WHERE c.id IN " +
              "(SELECT cu.id.idCampanha FROM CampanhaUsuario cu " +
              "WHERE cu.id.idUsuario = :usuarioId) " +
              "AND c.Status = :status")
       long countByUsuarioIdAndStatus(@Param("usuarioId") Long usuarioId,
                                          @Param("status") String status);

       @Query("SELECT COUNT(DISTINCT cu.id.idUsuario) FROM CampanhaUsuario cu " +
              "WHERE cu.id.idCampanha IN (" +
              "  SELECT cu2.id.idCampanha FROM CampanhaUsuario cu2 " +
              "  WHERE cu2.id.idUsuario = :idUsuario" +
              ") " +
              "AND cu.id.idUsuario <> :idUsuario")
       long countJogadoresNasMesmasCampanhas(@Param("idUsuario") Long idUsuario);

       @Query("SELECT c FROM Campanha c JOIN CampanhaUsuario cu ON cu.id.idCampanha = c.id " +
              "WHERE cu.id.idUsuario = :usuarioId")
       List<Campanha> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}