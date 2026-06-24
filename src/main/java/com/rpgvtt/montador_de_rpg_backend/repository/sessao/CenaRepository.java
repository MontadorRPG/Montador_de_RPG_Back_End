package com.rpgvtt.montador_de_rpg_backend.repository.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.PapeisUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Cena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CenaRepository extends JpaRepository<Cena, Long> {

    Optional<Cena> findTopBySessao_IdOrderByOrdemDesc(Long idSessao);

    List<Cena> findBySessao_IdOrderByOrdemDesc(Long idSessao);

    // Cenas onde o usuário é mestre da campanha da sessão da cena
    @Query("SELECT COUNT(c) FROM Cena c WHERE c.sessao.campanha.id IN " +
           "(SELECT cu.id.idCampanha FROM CampanhaUsuario cu " +
           "WHERE cu.id.idUsuario = :usuarioId AND cu.papel = :papel)")
    long countByCriadorId(@Param("usuarioId") Long usuarioId, 
                          @Param("papel") PapeisUsuario papel);

    // Cenas com campos nulos (exemplo: mapaJson ou urlMapa nulos)
    @Query("SELECT COUNT(c) FROM Cena c WHERE c.sessao.campanha.id IN " +
           "(SELECT cu.id.idCampanha FROM CampanhaUsuario cu " +
           "WHERE cu.id.idUsuario = :usuarioId AND cu.papel = :papel) " +
           "AND (c.mapaJson IS NULL OR c.urlMapa IS NULL)")
    long countByCriadorIdAndCamposNulos(@Param("usuarioId") Long usuarioId, 
                                        @Param("papel") PapeisUsuario papel);
}