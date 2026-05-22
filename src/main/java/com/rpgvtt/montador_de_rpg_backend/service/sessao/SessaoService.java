package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.SessaoCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.SessaoResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.repository.CampanhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final CampanhaRepository campanhaRepository;

    /**
     * Inicia uma nova sessão para uma campanha ativa.
     */
    @Transactional
    public SessaoResponseDTO criarSessao(SessaoCreateDTO dto) {
        Campanha campanha = campanhaRepository.findById(dto.campanhaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada."));

        Sessao sessao = new Sessao();
        sessao.setCampanha(campanha);
        // dataInicio é gerada automaticamente via @CreationTimestamp no banco

        Sessao sessaoSalva = sessaoRepository.save(sessao);
        return mapearParaDTO(sessaoSalva);
    }

    /**
     * Recupera os dados detalhados de uma sessão específica pelo ID.
     */
    @Transactional(readOnly = true)
    public SessaoResponseDTO buscarPorId(Long id) {
        Sessao sessao = sessaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada."));
        return mapearParaDTO(sessao);
    }

    /**
     * Encerra a sessão de jogo ativa, definindo a data de fim.
     */
    @Transactional
    public SessaoResponseDTO encerrarSessao(Long id) {
        Sessao sessao = sessaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada."));

        if (sessao.getDataFim() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta sessão já foi encerrada anteriormente.");
        }

        sessao.setDataFim(LocalDateTime.now());
        Sessao sessaoEncerrada = sessaoRepository.save(sessao);
        return mapearParaDTO(sessaoEncerrada);
    }

    /**
     * Método auxiliar de mapeamento manual (Entity -> DTO).
     * Ideal para evitar acoplamento com bibliotecas pesadas de mapeamento.
     */
    private SessaoResponseDTO mapearParaDTO(Sessao sessao) {
        return new SessaoResponseDTO(
                sessao.getId(),
                sessao.getCampanha().getId(), // Busca o ID diretamente da entidade relacionada
                sessao.getDataInicio(),
                sessao.getDataFim()
        );
    }
}