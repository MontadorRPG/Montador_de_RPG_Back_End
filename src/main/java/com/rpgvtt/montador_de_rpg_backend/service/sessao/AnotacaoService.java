package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Anotacao;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.AnotacaoCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.AnotacaoResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.AnotacaoUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.AnotacaoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnotacaoService {

    private final AnotacaoRepository repo;
    private final UsuarioRepository usuarioRepo;
    private final CampanhaRepository campanhaRepo;
    private final SessaoRepository sessaoRepo;

    public AnotacaoResponseDTO criar(AnotacaoCreateDTO dto) {
        Anotacao a = new Anotacao();
        if (dto.getUsuarioId() != null) a.setUsuario(usuarioRepo.findById(dto.getUsuarioId()).orElse(null));
        if (dto.getCampanhaId() != null) a.setCampanha(campanhaRepo.findById(dto.getCampanhaId()).orElse(null));
        if (dto.getSessaoId() != null) a.setSessao(sessaoRepo.findById(dto.getSessaoId()).orElse(null));
        a.setTitulo(dto.getTitulo());
        a.setConteudo(dto.getConteudo());
        a.setCategoria(dto.getCategoria());
        JsonNode complex = dto.getAnotacaoComplexa();
        a.setAnotacaoComplexa(complex);
        a.setEPrivado(dto.getEPrivado() == null ? Boolean.TRUE : dto.getEPrivado());

        Anotacao saved = repo.save(a);
        return toDTO(saved);
    }

    public AnotacaoResponseDTO atualizar(Integer id, AnotacaoUpdateDTO dto) {
        Anotacao a = repo.findById(id).orElseThrow(() -> new EntityNotFoundException(Anotacao.class, id));
        if (dto.getTitulo() != null) a.setTitulo(dto.getTitulo());
        if (dto.getConteudo() != null) a.setConteudo(dto.getConteudo());
        if (dto.getCategoria() != null) a.setCategoria(dto.getCategoria());
        if (dto.getAnotacaoComplexa() != null) a.setAnotacaoComplexa(dto.getAnotacaoComplexa());
        if (dto.getEPrivado() != null) a.setEPrivado(dto.getEPrivado());
        Anotacao saved = repo.save(a);
        return toDTO(saved);
    }

    public AnotacaoResponseDTO buscarPorId(Integer id) {
        Anotacao a = repo.findById(id).orElseThrow(() -> new EntityNotFoundException(Anotacao.class, id));
        return toDTO(a);
    }

    public List<AnotacaoResponseDTO> listarPorSessao(Long idSessao) {
        return repo.findBySessao_Id(idSessao).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public void deletar(Integer id) {
        repo.deleteById(id);
    }

    private AnotacaoResponseDTO toDTO(Anotacao a) {
        Long usuarioId = a.getUsuario() != null ? a.getUsuario().getId() : null;
        Long campanhaId = a.getCampanha() != null ? a.getCampanha().getId() : null;
        Long sessaoId = a.getSessao() != null ? a.getSessao().getId() : null;
        JsonNode complex = a.getAnotacaoComplexa();
        return new AnotacaoResponseDTO(a.getIdAnotacao(), usuarioId, campanhaId, sessaoId,
                a.getTitulo(), a.getConteudo(), a.getCategoria(), complex, a.getEPrivado());
    }
}
