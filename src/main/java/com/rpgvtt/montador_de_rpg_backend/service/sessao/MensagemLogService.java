package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.MensagemLog;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.MensagemCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.MensagemResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.MensagemLogRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MensagemLogService {

    private final MensagemLogRepository repo;
    private final SessaoRepository sessaoRepo;
    private final UsuarioRepository usuarioRepo;
    private final CampanhaRepository campanhaRepo;

    public MensagemResponseDTO criar(MensagemCreateDTO dto) {
        MensagemLog m = new MensagemLog();
        m.setSessao(sessaoRepo.findById(dto.getSessaoId()).orElseThrow(() -> new EntityNotFoundException(com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao.class, dto.getSessaoId())));
        m.setUsuario(usuarioRepo.findById(dto.getUsuarioId()).orElseThrow(() -> new EntityNotFoundException(com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario.class, dto.getUsuarioId())));
        if (dto.getCampanhaId() != null) m.setCampanha(campanhaRepo.findById(dto.getCampanhaId()).orElse(null));
        m.setConteudo(dto.getConteudo());
        MensagemLog saved = repo.save(m);
        return toDTO(saved);
    }

    public MensagemResponseDTO buscarPorId(Long id) {
        MensagemLog m = repo.findById(id).orElseThrow(() -> new EntityNotFoundException(MensagemLog.class, id));
        return toDTO(m);
    }

    public List<MensagemResponseDTO> listarPorSessao(Long idSessao) {
        return repo.findAll().stream()
                .filter(m -> m.getSessao() != null && m.getSessao().getId().equals(idSessao))
                .map(this::toDTO).collect(Collectors.toList());
    }

    public void deletar(Long id) {
        repo.deleteById(id);
    }

    private MensagemResponseDTO toDTO(MensagemLog m) {
        Long sessaoId = m.getSessao() != null ? m.getSessao().getId() : null;
        Long usuarioId = m.getUsuario() != null ? m.getUsuario().getId() : null;
        Long campanhaId = m.getCampanha() != null ? m.getCampanha().getId() : null;
        return new MensagemResponseDTO(m.getId(), sessaoId, usuarioId, campanhaId, m.getConteudo(), m.getMomento());
    }
}
