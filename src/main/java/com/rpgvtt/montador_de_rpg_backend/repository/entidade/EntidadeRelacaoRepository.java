package com.rpgvtt.montador_de_rpg_backend.repository.entidade;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeRelacao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeRelacaoKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntidadeRelacaoRepository extends JpaRepository<EntidadeRelacao, EntidadeRelacaoKey> {
    
// Busca todas as relações onde o id da chave tem idEntidadePai = :idPai
    List<EntidadeRelacao> findById_IdEntidadePai(Long idPai);

    // Busca a relação exata usando os dois campos da chave
    EntidadeRelacao findById_IdEntidadePaiAndId_IdEntidadeFilha(Long idPai, Long idFilha);

}