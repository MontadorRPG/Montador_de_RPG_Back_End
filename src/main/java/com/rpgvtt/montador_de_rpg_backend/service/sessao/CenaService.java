package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusSessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Cena;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.CenaParticipantes;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.CenaParticipantesKey;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaCombateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.IniciarCenaRequestDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.ProcedimentoContextoDTO;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
// import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.ProcedimentoEngine;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaParticipantesRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.ProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.service.CampanhaAutorizacao;
import com.rpgvtt.montador_de_rpg_backend.service.exceptions.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CenaService {

    private final CenaRepository cenaRepo;
    private final CenaParticipantesRepository cenaParticipantesRepo;
    private final SessaoRepository sessaoRepo;
    private final EntidadeInstanciaRepository instanciaRepo;
    private final ProcedimentoEngine procedimentoEngine;
    private final ProcedimentoRepository procedimentoRepo;
    private final CampanhaAutorizacao autorizacao;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper mapper;

    // ========== CRUD Básico (mantido do original) ==========

    public CenaResponseDTO criar(CenaCreateDTO dto) {
        Cena c = new Cena();
        c.setSessao(sessaoRepo.findById(dto.getSessaoId())
                .orElseThrow(() -> new EntityNotFoundException(Sessao.class, dto.getSessaoId())));
        c.setMapaJson(dto.getMapaJson());
        c.setUrlMapa(dto.getUrlMapa());
        c.setOrdem(dto.getOrdem());
        c.setTipo(dto.getTipo() != null ? dto.getTipo() : "NORMAL");
        // Inicializa estado vazio se não fornecido
        if (dto.getEstado() != null) {
            c.setEstado(dto.getEstado());
        } else {
            c.setEstado(mapper.createObjectNode());
        }
        Cena saved = cenaRepo.save(c);
        return toDTO(saved);
    }

    public CenaResponseDTO buscarPorId(Long id) {
        Cena c = cenaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Cena.class, id));
        return toDTO(c);
    }

    public List<CenaResponseDTO> listarPorSessao(Long idSessao) {
        return cenaRepo.findAll().stream()
                .filter(c -> c.getSessao() != null && c.getSessao().getId().equals(idSessao))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CenaResponseDTO atualizar(Long id, CenaUpdateDTO dto) {
        Cena c = cenaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Cena.class, id));
        if (dto.getMapaJson() != null) c.setMapaJson(dto.getMapaJson());
        if (dto.getUrlMapa() != null) c.setUrlMapa(dto.getUrlMapa());
        if (dto.getOrdem() != null) c.setOrdem(dto.getOrdem());
        if (dto.getTipo() != null) c.setTipo(dto.getTipo());
        if (dto.getEstado() != null) c.setEstado(dto.getEstado());
        Cena saved = cenaRepo.save(c);
        return toDTO(saved);
    }

    public void deletar(Long id) {
        cenaRepo.deleteById(id);
    }


    @Transactional
    public void atualizarPosicaoToken(Long idSessao, Long idInstancia, double x, double y) {
        // Busca todas as cenas da sessão ordenadas pela maior ordem (a mais recente primeiro)
        List<Cena> cenas = cenaRepo.findBySessao_IdOrderByOrdemDesc(idSessao);
        
        CenaParticipantes participante = null;
        Cena cenaAtiva = null;
        
        for (Cena cena : cenas) {
            CenaParticipantesKey key = new CenaParticipantesKey(cena.getId(), idInstancia);
            Optional<CenaParticipantes> opt = cenaParticipantesRepo.findById(key);
            if (opt.isPresent()) {
                participante = opt.get();
                cenaAtiva = cena;
                break;
            }
        }
        
        if (participante == null || cenaAtiva == null) {
            throw new EntityNotFoundException(CenaParticipantes.class, 
                    "Instância " + idInstancia + " não está em nenhuma cena da sessão " + idSessao);
        }

        ObjectNode posicao = participante.getPosicao() instanceof ObjectNode on 
                ? on : mapper.createObjectNode();
        posicao.put("x", x);
        posicao.put("y", y);
        participante.setPosicao(posicao);
        cenaParticipantesRepo.save(participante);
    }


    // ========== Gerenciamento de Cena de Combate ==========

    /**
     * Inicia uma cena de combate com participantes e procedimento.
     */
    public CenaCombateDTO iniciarCena(IniciarCenaRequestDTO req, Long idMestre) {
        Sessao sessao = exigirSessaoAtiva(req.idSessao());
        Long idCampanha = sessao.getCampanha().getId();
        autorizacao.exigirMestre(idCampanha, idMestre);

        List<EntidadeInstancia> instanciasJogadores =
                resolverInstancias(req.idsInstanciasJogadores(), idCampanha, "jogadores");
        List<EntidadeInstancia> instanciasInimigos  =
                resolverInstancias(req.idsInstanciasInimigos(), idCampanha, "inimigos");

        if (instanciasJogadores.isEmpty() && instanciasInimigos.isEmpty()) {
            throw new EstadoInvalidoException("Uma cena precisa de ao menos um participante");
        }

        // Criar a Cena
        Cena cena = new Cena();
        cena.setSessao(sessao);
        cena.setTipo(req.tipo() != null ? req.tipo() : "COMBATE");
        cena.setOrdem(req.ordem() != null ? req.ordem() : 0);
        cena.setMapaJson(null); // pode ser definido depois
        cena.setUrlMapa(null);

        // Inicializa o estado da cena
        ObjectNode estado = mapper.createObjectNode();
        estado.put("combateAtivo", true);
        estado.put("rodada", 0);
        estado.put("turnoAtualId", (String) null);
        estado.set("jaAgiramIds", mapper.createArrayNode());
        estado.set("dadosDisponiveis", mapper.createObjectNode());
        cena.setEstado(estado);
        cenaRepo.save(cena);

        // Adicionar participantes
        addParticipantes(cena, instanciasJogadores, 0);
        addParticipantes(cena, instanciasInimigos, 1);
        if (req.lados() != null) {
            for (int i = 0; i < req.lados().size(); i++) {
                List<EntidadeInstancia> lado =
                        resolverInstancias(req.lados().get(i), idCampanha, "lado " + (i + 2));
                addParticipantes(cena, lado, i + 2);
            }
        }

        // Iniciar procedimento de combate
        Procedimento procCombate = procedimentoRepo
                .findByTipoAndSistemaId("INICIAR_COMBATE", sessao.getCampanha().getSistema().getId())
                .orElseThrow(() -> new ConfiguracaoException("Procedimento INICIAR_COMBATE não encontrado"));

        List<Long> todosIds = Stream.concat(
                instanciasJogadores.stream(),
                instanciasInimigos.stream()
        ).map(EntidadeInstancia::getId).toList();

        ProcedimentoContexto ctxCena = procedimentoEngine.iniciarComMultiplos(
                procCombate.getId(), sessao.getId(), todosIds);

        // Armazenar id_cena no contexto
        ctxCena.getContexto().put("id_cena", cena.getId());

        // Atualizar estado da cena para EM_ANDAMENTO
        estado.put("combateAtivo", true);
        cena.setEstado(estado);
        cenaRepo.save(cena);

        // Broadcast
        broadcastCena(sessao.getId(), "CENA_INICIADA", Map.of(
                "idCena", cena.getId(),
                "participantesTime0", toParticipanteDTO(instanciasJogadores),
                "participantesTime1", toParticipanteDTO(instanciasInimigos),
                "estadoProcedimento", ProcedimentoContextoDTO.from(ctxCena)
        ));

        log.info("Cena {} iniciada na sessão {} com {} jogadores e {} inimigos",
                cena.getId(), req.idSessao(), instanciasJogadores.size(), instanciasInimigos.size());

        return toCombateDTO(cena);
    }

    /**
     * Adiciona um participante a uma cena em andamento.
     */
    public void adicionarParticipante(Long idCena, Long idInstancia, int lado, Long idMestre) {
        Cena cena = cenaRepo.findById(idCena)
                .orElseThrow(() -> new EntityNotFoundException(Cena.class, idCena));
        autorizacao.exigirMestre(cena.getSessao().getCampanha().getId(), idMestre);

        EntidadeInstancia inst = instanciaRepo.findById(idInstancia)
                .orElseThrow(() -> new EntityNotFoundException(EntidadeInstancia.class, idInstancia));
        addParticipantes(cena, List.of(inst), lado);

        broadcastCena(cena.getSessao().getId(), "PARTICIPANTE_ADICIONADO",
                Map.of("idCena", idCena, "idInstancia", idInstancia, "lado", lado));
    }

    /**
     * Remove um participante da cena.
     */
    public void removerParticipante(Long idCena, Long idInstancia, Long idMestre) {
        Cena cena = cenaRepo.findById(idCena)
                .orElseThrow(() -> new EntityNotFoundException(Cena.class, idCena));
        autorizacao.exigirMestre(cena.getSessao().getCampanha().getId(), idMestre);

        CenaParticipantesKey id = new CenaParticipantesKey(idCena, idInstancia);
        cenaParticipantesRepo.findById(id).ifPresent(cp -> {
            cenaParticipantesRepo.delete(cp);
        });

        broadcastCena(cena.getSessao().getId(), "PARTICIPANTE_REMOVIDO",
                Map.of("idCena", idCena, "idInstancia", idInstancia));
    }

    /**
     * Encerra uma cena de combate.
     */
    public void encerrarCena(Long idCena, String motivo, Long idMestre) {
        Cena cena = cenaRepo.findById(idCena).orElseThrow();
        autorizacao.exigirMestre(cena.getSessao().getCampanha().getId(), idMestre);

        ObjectNode estado = (ObjectNode) cena.getEstado();
        if (estado == null) estado = mapper.createObjectNode();
        estado.put("combateAtivo", false);
        cena.setEstado(estado);
        cenaRepo.save(cena);

        broadcastCena(cena.getSessao().getId(), "CENA_ENCERRADA",
                Map.of("idCena", idCena, "motivo", motivo));
    }

    // ========== Métodos auxiliares ==========

    private List<EntidadeInstancia> resolverInstancias(List<Long> ids, Long idCampanha, String contexto) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<EntidadeInstancia> instancias = instanciaRepo.findAllById(ids);
        if (instancias.size() != ids.size()) {
            Set<Long> encontrados = instancias.stream()
                    .map(EntidadeInstancia::getId)
                    .collect(Collectors.toSet());
            List<Long> ausentes = ids.stream()
                    .filter(id -> !encontrados.contains(id))
                    .toList();
            throw new RecursoNaoEncontradoException(
                    "Instâncias não encontradas para " + contexto + ": " + ausentes);
        }
        List<Long> foraDaCampanha = instancias.stream()
                .filter(i -> !i.getCampanha().getId().equals(idCampanha))
                .map(EntidadeInstancia::getId)
                .toList();
        if (!foraDaCampanha.isEmpty()) {
            throw new DeniedAcessException(
                    "Instâncias não pertencem à campanha " + idCampanha + ": " + foraDaCampanha);
        }
        return instancias;
    }

    private void addParticipantes(Cena cena, List<EntidadeInstancia> instancias, int lado) {
        for (int i = 0; i < instancias.size(); i++) {
            CenaParticipantes cp = new CenaParticipantes();
            cp.setId(new CenaParticipantesKey(cena.getId(), instancias.get(i).getId()));
            cp.setCena(cena);
            cp.setEntidadeInstancia(instancias.get(i));
            cp.setLado(lado);
            cp.setPosicao(null);
            cenaParticipantesRepo.save(cp);
        }
    }

    private Sessao exigirSessaoAtiva(Long idSessao) {
        Sessao sessao = sessaoRepo.findById(idSessao)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada: " + idSessao));
        if (sessao.getStatus() != StatusSessao.ATIVA) {
            throw new EstadoInvalidoException("Sessão " + idSessao + " não está ativa");
        }
        return sessao;
    }

    private void broadcastCena(Long idSessao, String tipo, Map<String, Object> payload) {
        messagingTemplate.convertAndSend(
                "/topic/sessao/" + idSessao,
                (Object) Map.of("tipo", tipo, "payload", payload)
        );
    }

    private List<Map<String, Object>> toParticipanteDTO(List<EntidadeInstancia> instancias) {
        return instancias.stream().map(i -> Map.<String, Object>of(
                "idInstancia", i.getId(),
                "nome",        i.getNome(),
                "tipo",        i.getTipo()
        )).toList();
    }

    // ========== DTO Builders ==========

    private CenaResponseDTO toDTO(Cena c) {
        Long sessaoId = c.getSessao() != null ? c.getSessao().getId() : null;
        return new CenaResponseDTO(
                c.getId(),
                sessaoId,
                c.getMapaJson(),
                c.getUrlMapa(),
                c.getOrdem(),
                c.getTipo(),
                c.getEstado()
        );
    }

    private CenaCombateDTO toCombateDTO(Cena c) {
        int rodada = 0;
        String status = "DESCONHECIDO";
        if (c.getEstado() != null) {
            if (c.getEstado().has("rodada")) {
                rodada = c.getEstado().get("rodada").asInt();
            }
            if (c.getEstado().has("combateAtivo")) {
                status = c.getEstado().get("combateAtivo").asBoolean() ? "EM_ANDAMENTO" : "CONCLUIDO";
            }
        }
        return new CenaCombateDTO(c.getId(), status, rodada);
    }
}