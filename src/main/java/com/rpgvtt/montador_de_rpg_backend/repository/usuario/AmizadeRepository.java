package com.rpgvtt.montador_de_rpg_backend.repository.usuario;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusAmizade;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Amizade;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.AmizadeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmizadeRepository extends JpaRepository<Amizade, AmizadeKey> {

    @Query("""
        SELECT a FROM Amizade a
        WHERE (a.id.idRemetente = :idA AND a.id.idDestinatario = :idB)
           OR (a.id.idRemetente = :idB AND a.id.idDestinatario = :idA)
    """)
    Optional<Amizade> findEntreUsuarios(@Param("idA") Long idA, @Param("idB") Long idB);

    @Query("""
        SELECT a FROM Amizade a
        WHERE (a.id.idRemetente = :idUsuario OR a.id.idDestinatario = :idUsuario)
          AND a.status = :status
    """)
    List<Amizade> findByUsuarioAndStatus(@Param("idUsuario") Long idUsuario,
                                         @Param("status") StatusAmizade status);


    List<Amizade> findById_IdDestinatarioAndStatus(Long idDestinatario, StatusAmizade status);
}