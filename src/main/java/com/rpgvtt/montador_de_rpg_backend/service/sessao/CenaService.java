package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Cena;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CenaService {

    private final CenaRepository repo;
    private final SessaoRepository sessaoRepo;

    public CenaResponseDTO criar(CenaCreateDTO dto) {
        Cena c = new Cena();
        c.setSessao(sessaoRepo.findById(dto.getSessaoId()).orElseThrow(() -> new EntityNotFoundException(com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao.class, dto.getSessaoId())));
        c.setMapaJson(dto.getMapaJson());
        c.setUrlMapa(dto.getUrlMapa());
        c.setOrdem(dto.getOrdem());
        Cena saved = repo.save(c);
        return toDTO(saved);
    }

    public CenaResponseDTO buscarPorId(Long id) {
        Cena c = repo.findById(id).orElseThrow(() -> new EntityNotFoundException(Cena.class, id));
        return toDTO(c);
    }

    public List<CenaResponseDTO> listarPorSessao(Long idSessao) {
        return repo.findAll().stream()
                .filter(c -> c.getSessao() != null && c.getSessao().getId().equals(idSessao))
                .map(this::toDTO).collect(Collectors.toList());
    }

    public CenaResponseDTO atualizar(Long id, CenaUpdateDTO dto) {
        Cena c = repo.findById(id).orElseThrow(() -> new EntityNotFoundException(Cena.class, id));
        JsonNode mapa = dto.getMapaJson();
        if (mapa != null) c.setMapaJson(mapa);
        if (dto.getUrlMapa() != null) c.setUrlMapa(dto.getUrlMapa());
        if (dto.getOrdem() != null) c.setOrdem(dto.getOrdem());
        Cena saved = repo.save(c);
        return toDTO(saved);
    }

    public void deletar(Long id) {
        repo.deleteById(id);
    }

    private CenaResponseDTO toDTO(Cena c) {
        Long sessaoId = c.getSessao() != null ? c.getSessao().getId() : null;
        return new CenaResponseDTO(c.getId(), sessaoId, c.getMapaJson(), c.getUrlMapa(), c.getOrdem());
    }
}
