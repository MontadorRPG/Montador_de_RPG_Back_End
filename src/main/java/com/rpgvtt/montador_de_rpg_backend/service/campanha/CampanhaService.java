package com.rpgvtt.montador_de_rpg_backend.service.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.PapeisUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusCampanha;
import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusSessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuarioKey;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.AdicionarJogadorDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaPapelDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaParticipanteResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaSessaoTemporariaDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.PersonagemCampanhaDTO;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaUsuarioRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.personagem.PersonagemRepository; // <-- Import adicionado
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.usuario.UsuarioRepository;
import com.rpgvtt.montador_de_rpg_backend.service.usuario.UsuarioService;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampanhaService {

    private final CampanhaRepository campanhaRepository;
    private final CampanhaUsuarioRepository campanhaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SessaoRepository sessaoRepository; 
    private final EntityManager entityManager;
    private final EntidadeInstanciaRepository instanciaRepository;
    private final PersonagemRepository personagemRepository;
    private final UsuarioService usuarioService;


    // public CampanhaService(CampanhaRepository campanhaRepository,
    //                        CampanhaUsuarioRepository campanhaUsuarioRepository,
    //                        UsuarioRepository usuarioRepository,
    //                        SessaoRepository sessaoRepository, 
    //                        EntityManager entityManager,
    //                        EntidadeInstanciaRepository instanciaRepository,
    //                        PersonagemRepository personagemRepository
    //                       ) {
    //     this.campanhaRepository = campanhaRepository;
    //     this.campanhaUsuarioRepository = campanhaUsuarioRepository;
    //     this.usuarioRepository = usuarioRepository;
    //     this.sessaoRepository = sessaoRepository; 
    //     this.entityManager = entityManager;
    //     this.instanciaRepository = instanciaRepository;
    //     this.personagemRepository = personagemRepository; // <-- Atribuição adicionada
    // }

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
    public void deletar(Long id, Long usuarioLogadoId) {
        if (!campanhaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada");
        }

        CampanhaUsuarioKey keyUsuario = new CampanhaUsuarioKey(id, usuarioLogadoId);
        CampanhaUsuario participacao = campanhaUsuarioRepository.findById(keyUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa desta campanha"));

        if (participacao.getPapel() != PapeisUsuario.MESTRE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: Apenas o mestre pode deletar a campanha");
        }

        entityManager.createQuery("DELETE FROM CampanhaUsuario cu WHERE cu.id.idCampanha = :campanhaId")
                .setParameter("campanhaId", id)
                .executeUpdate();

        entityManager.createQuery("UPDATE EntidadeInstancia e SET e.campanha = null WHERE e.campanha.id = :campanhaId")
                .setParameter("campanhaId", id)
                .executeUpdate();

        entityManager.createQuery("UPDATE Personagem p SET p.campanha = null WHERE p.campanha.id = :campanhaId")
                .setParameter("campanhaId", id)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM Sessao s WHERE s.campanha.id = :campanhaId")
                .setParameter("campanhaId", id)
                .executeUpdate();

        campanhaRepository.deleteById(id);
    }

    @Transactional
    public void deletarTemporaria(Long id, Long usuarioId) {
        Campanha campanha = campanhaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Campanha não encontrada"));

        // só deleta se for temporária (nome começa com "Temp-Char-")
        if (!campanha.getNome().startsWith("Temp-Char-")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Esta campanha não é temporária");
        }

        entityManager.createQuery(
                "DELETE FROM CampanhaUsuario cu WHERE cu.id.idCampanha = :id")
                .setParameter("id", id).executeUpdate();

        entityManager.createQuery(
                "DELETE FROM Sessao s WHERE s.campanha.id = :id")
                .setParameter("id", id).executeUpdate();

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
                novoVinculo.getEntrouEm(),
                novoVinculo.getUsuario().getApelido()
        );
    }

    @Transactional
    public void removerJogador(Long campanhaId, Long usuarioLogadoId, Long usuarioParaRemoverId) {
        
        CampanhaUsuarioKey keyMestre = new CampanhaUsuarioKey(campanhaId, usuarioLogadoId);
        CampanhaUsuario mestre = campanhaUsuarioRepository.findById(keyMestre)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa desta campanha"));

        if (mestre.getPapel() != PapeisUsuario.MESTRE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o mestre pode remover jogadores");
        }

        
        CampanhaUsuarioKey keyRemover = new CampanhaUsuarioKey(campanhaId, usuarioParaRemoverId);
        if (!campanhaUsuarioRepository.existsById(keyRemover)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Jogador não encontrado nesta campanha");
        }

       
        campanhaUsuarioRepository.deleteById(keyRemover);
    }

    @Transactional(readOnly = true)
    public Optional<CampanhaPapelDTO> obterMinhaRole(Long campanhaId, Long usuarioId) {
        CampanhaUsuarioKey key = new CampanhaUsuarioKey(campanhaId, usuarioId);
        
        return campanhaUsuarioRepository.findById(key)
                .map(cu -> new CampanhaPapelDTO(cu.getPapel().name()));
    }

    @Transactional
    public CampanhaResponseDTO atualizar(Long id, CampanhaUpdateDTO dto) {
        Campanha campanha = campanhaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada"));

        if (dto.nome() != null) campanha.setNome(dto.nome());
        if (dto.descricao() != null) campanha.setDescricao(dto.descricao());
        if (dto.urlImagem() != null) campanha.setUrlImagem(dto.urlImagem());
        if (dto.status() != null) {
            campanha.setStatus(StatusCampanha.valueOf(dto.status().toUpperCase()));
        }
        if (dto.sistemaId() != null) {
            campanha.setSistema(
                entityManager.getReference(Sistema.class, dto.sistemaId())
            );
        }

        campanha = campanhaRepository.save(campanha);
        return mapToResponseDTO(campanha);
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
                campanha.getSistema().getNome(),
                campanha.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public List<CampanhaParticipanteResponseDTO> listarParticipantes(Long campanhaId) {
        return campanhaUsuarioRepository.findByCampanhaId(campanhaId)
                .stream()
                .map(cu -> new CampanhaParticipanteResponseDTO(
                        cu.getId().getIdCampanha(),
                        cu.getId().getIdUsuario(),
                        cu.getPapel().name(),
                        cu.getEntrouEm(),
                        cu.getUsuario().getApelido()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PersonagemCampanhaDTO> buscarPersonagemDoUsuario(Long campanhaId, Long usuarioId) {
        return personagemRepository.findAtivoByCampanhaIdAndUsuarioId(campanhaId, usuarioId)
                .map(p -> new PersonagemCampanhaDTO(
                        p.getId(),
                        p.getInstancia().getId(),
                        p.getInstancia().getNome(),
                        p.getInstancia().getTipo()
                ));
    }

    @Transactional
    public PersonagemCampanhaDTO vincularPersonagem(Long campanhaId, Long instanciaId, Long usuarioId) {
        if (!campanhaRepository.existsById(campanhaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada");
        }

        EntidadeInstancia instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Instância não encontrada"));

        // Verifica se o usuário já tem personagem ativo nesta campanha
        personagemRepository.findAtivoByCampanhaIdAndUsuarioId(campanhaId, usuarioId)
                .ifPresent(p -> { throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Você já tem um personagem nesta campanha"); });

        Personagem personagem = new Personagem();
        personagem.setCampanha(entityManager.getReference(Campanha.class, campanhaId));
        personagem.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
        personagem.setInstancia(instancia);
        personagem.setAtivo(true);
        personagemRepository.save(personagem);

        return new PersonagemCampanhaDTO(
                personagem.getId(),
                instancia.getId(),
                instancia.getNome(),
                instancia.getTipo()
        );
    }
}