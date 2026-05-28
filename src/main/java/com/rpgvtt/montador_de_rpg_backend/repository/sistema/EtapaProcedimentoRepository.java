package com.rpgvtt.montador_de_rpg_backend.repository.sistema;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtapaProcedimentoRepository extends JpaRepository<EtapaProcedimento, Long> {

    List<EtapaProcedimento> findByIdProcedimentoOrderByOrdem(Long idFilho);
}