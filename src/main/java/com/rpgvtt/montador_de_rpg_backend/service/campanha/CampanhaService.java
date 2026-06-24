package com.rpgvtt.montador_de_rpg_backend.service.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.PapeisUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusCampanha;
import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusSessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuarioKey;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.AdicionarJogadorDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaParticipanteResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaSessaoTemporariaDTO;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaUsuarioRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CampanhaService {

    private final CampanhaRepository campanhaRepository;
    private final CampanhaUsuarioRepository campanhaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SessaoRepository sessaoRepository; 
    private final EntityManager entityManager;

    public CampanhaService(CampanhaRepository campanhaRepository,
                           CampanhaUsuarioRepository campanhaUsuarioRepository,
                           UsuarioRepository usuarioRepository,
                           SessaoRepository sessaoRepository, 
                           EntityManager entityManager) {
        this.campanhaRepository = campanhaRepository;
        this.campanhaUsuarioRepository = campanhaUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.sessaoRepository = sessaoRepository; 
        this.entityManager = entityManager;
    }

    @Transactional
    public CampanhaResponseDTO criar(CampanhaCreateDTO dto) {
        Campanha campanha = new Campanha();
        campanha.setNome(dto.nome());
        campanha.setSistema(entityManager.getReference(Sistema.class, dto.sistemaId()));
        campanha.setStatus(StatusCampanha.ATIVA);
        campanha = campanhaRepository.save(campanha);

        Usuario usuarioProxy = entityManager.getReference(Usuario.class, dto.criadorId());
        CampanhaUsuarioKey vinculoKey = new CampanhaUsuarioKey(campanha.getId(), dto.criadorId());
        CampanhaUsuario vinculo = new CampanhaUsuario();
        vinculo.setId(vinculoKey);
        vinculo.setCampanha(campanha);
        vinculo.setUsuario(usuarioProxy);
        vinculo.setPapel(PapeisUsuario.MESTRE);
        campanhaUsuarioRepository.save(vinculo);

        return mapToResponseDTO(campanha);
    }

    @Transactional
    public CampanhaSessaoTemporariaDTO criarTemporariaComSessao(Long sistemaId, Long usuarioId) {
        // 1. Campanha
        Campanha campanha = new Campanha();
        campanha.setNome("Temp-Char-" + System.currentTimeMillis());
        campanha.setSistema(entityManager.getReference(Sistema.class, sistemaId));
        campanha.setStatus(StatusCampanha.ATIVA);
        campanha = campanhaRepository.save(campanha);

        // 2. Vínculo MESTRE — mesma transação
        CampanhaUsuarioKey key = new CampanhaUsuarioKey(campanha.getId(), usuarioId);
        CampanhaUsuario vinculo = new CampanhaUsuario();
        vinculo.setId(key);
        vinculo.setCampanha(campanha);
        vinculo.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
        vinculo.setPapel(PapeisUsuario.MESTRE);
        campanhaUsuarioRepository.save(vinculo);

        // 3. Sessão — mesma transação, sem verificação de mestre
        Sessao sessao = new Sessao();
        sessao.setCampanha(campanha);
        sessao.setStatus(StatusSessao.ATIVA);
        sessao.setDataInicio(LocalDateTime.now());
        sessao.setOrdem(1);
        sessao = sessaoRepository.save(sessao);

        return new CampanhaSessaoTemporariaDTO(campanha.getId(), sessao.getId());
    }

    @Transactional(readOnly = true)
    public CampanhaResponseDTO buscarPorId(Long id) {
        Campanha campanha = campanhaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada"));
        return mapToResponseDTO(campanha);
    }

    @Transactional(readOnly = true)
    public List<CampanhaResponseDTO> listarTodas() {
        return campanhaRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional
    public void deletar(Long id) {
        if (!campanhaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada");
        }
        campanhaRepository.deleteById(id);
    }

    @Transactional
    public CampanhaParticipanteResponseDTO adicionarJogador(Long campanhaId, AdicionarJogadorDTO dto) {
        if (!campanhaRepository.existsById(campanhaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada");
        }
        if (!usuarioRepository.existsById(dto.usuarioId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }

        CampanhaUsuarioKey key = new CampanhaUsuarioKey(campanhaId, dto.usuarioId());
        if (campanhaUsuarioRepository.existsById(key)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário já está participando desta campanha");
        }

        CampanhaUsuario novoVinculo = new CampanhaUsuario();
        novoVinculo.setId(key);
        novoVinculo.setCampanha(entityManager.getReference(Campanha.class, campanhaId));
        novoVinculo.setUsuario(entityManager.getReference(Usuario.class, dto.usuarioId()));
        novoVinculo.setPapel(PapeisUsuario.JOGADOR);
        novoVinculo = campanhaUsuarioRepository.save(novoVinculo);

        return new CampanhaParticipanteResponseDTO(
                novoVinculo.getId().getIdCampanha(),
                novoVinculo.getId().getIdUsuario(),
                novoVinculo.getPapel().name(),
                novoVinculo.getEntrouEm()
        );
    }

    @Transactional(readOnly = true)
    public List<CampanhaResponseDTO> listarPorUsuario(Long usuarioId) {
        return campanhaRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    private CampanhaResponseDTO mapToResponseDTO(Campanha campanha) {
        return new CampanhaResponseDTO(
                campanha.getId(),
                campanha.getNome(),
                campanha.getCriadaEm(),
                campanha.getSistema().getId(),
                campanha.getSistema().getNome()
        );
    }
}