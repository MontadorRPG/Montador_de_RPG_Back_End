// package com.rpgvtt.montador_de_rpg_backend.service.batalha;

// import com.rpgvtt.montador_de_rpg_backend.domain.enums.BatalhaStatus;
// import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusSessao;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.batalha.Batalha;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.batalha.BatalhaParticipantes;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.batalha.BatalhaParticipantesKey;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
// import com.rpgvtt.montador_de_rpg_backend.dto.batalha.BatalhaDTO;
// import com.rpgvtt.montador_de_rpg_backend.dto.batalha.IniciarBatalhaRequestDTO;
// import com.rpgvtt.montador_de_rpg_backend.dto.sistema.ProcedimentoContextoDTO;
// import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
// import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.ProcedimentoEngine;
// import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
// import com.rpgvtt.montador_de_rpg_backend.repository.batalha.BatalhaParticipantesRepository;
// import com.rpgvtt.montador_de_rpg_backend.repository.batalha.BatalhaRepository;
// import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
// import com.rpgvtt.montador_de_rpg_backend.repository.personagem.PersonagemRepository;
// import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
// import com.rpgvtt.montador_de_rpg_backend.repository.sistema.ProcedimentoRepository;
// import com.rpgvtt.montador_de_rpg_backend.service.CampanhaAutorizacao;
// import com.rpgvtt.montador_de_rpg_backend.service.exceptions.ConfiguracaoException;
// import com.rpgvtt.montador_de_rpg_backend.service.exceptions.DeniedAcessException;
// import com.rpgvtt.montador_de_rpg_backend.service.exceptions.EstadoInvalidoException;
// import com.rpgvtt.montador_de_rpg_backend.service.exceptions.RecursoNaoEncontradoException;
// import jakarta.transaction.Transactional;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.stereotype.Service;

// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import java.util.stream.Collectors;
// import java.util.stream.Stream;

// @Service
// @RequiredArgsConstructor
// @Transactional
// @Slf4j
// public class BatalhaService {

//     private final BatalhaRepository batalhaRepo;
//     private final BatalhaParticipantesRepository batalhaInstanciaRepo;
//     private final SessaoRepository sessaoRepo;
//     private final EntidadeInstanciaRepository instanciaRepo;
//     private final PersonagemRepository personagemRepo;
//     private final ProcedimentoEngine procedimentoEngine;
//     private final ProcedimentoRepository procedimentoRepo;
//     private final CampanhaAutorizacao autorizacao;
//     private final SimpMessagingTemplate messagingTemplate;

//     public BatalhaDTO iniciarBatalha(IniciarBatalhaRequestDTO req, Long idMestre) {
//         Sessao sessao = exigirSessaoAtiva(req.idSessao());
//         Long idCampanha = sessao.getCampanha().getId();
//         autorizacao.exigirMestre(idCampanha, idMestre);

//         // Validate all requested instances belong to this campaign
//         List<EntidadeInstancia> instanciasJogadores =
//                 resolverInstancias(req.idsInstanciasJogadores(), idCampanha, "jogadores");
//         List<EntidadeInstancia> instanciasInimigos  =
//                 resolverInstancias(req.idsInstanciasInimigos(),  idCampanha, "inimigos");

//         if (instanciasJogadores.isEmpty() && instanciasInimigos.isEmpty()) {
//             throw new EstadoInvalidoException("Uma batalha precisa de ao menos um participante");
//         }

//         // Create the Batalha record
//         Batalha batalha = new Batalha();
//         batalha.setSessao(sessao);
//         batalha.setStatus(BatalhaStatus.INICIANDO);
//         batalha.setRodadaAtual(0);
// //        if (req.idCena() != null) {
// //            batalha.setCena(new Cena(req.idCena())); // lazy ref
// //        }
//         batalhaRepo.save(batalha);

//         // Add player instances (lado 0)
//         addParticipantes(batalha, instanciasJogadores, 0);
//         // Add enemy instances (lado 1)
//         addParticipantes(batalha, instanciasInimigos, 1);
//         // Add extra sides if provided (PvP, neutral, etc.)
//         if (req.lados() != null) {
//             for (int i = 0; i < req.lados().size(); i++) {
//                 List<EntidadeInstancia> lado =
//                         resolverInstancias(req.lados().get(i), idCampanha, "lado " + (i + 2));
//                 addParticipantes(batalha, lado, i + 2);
//             }
//         }

//         // Start the combat procedure for this system
//         Procedimento procBatalha = procedimentoRepo
//                 .findByTipoAndSistemaId(
//                         "INICIAR_COMBATE",
//                         sessao.getCampanha().getSistema().getId())
//                 .orElseThrow(() -> new ConfiguracaoException(
//                         "Procedimento SESSAO_COMBATE não encontrado"));

//         // Collect all participant IDs for the MULTIPLAS scope
//         List<Long> todosIds = Stream.concat(
//                 instanciasJogadores.stream(),
//                 instanciasInimigos.stream()
//         ).map(EntidadeInstancia::getId).toList();

//         ProcedimentoContexto ctxBatalha = procedimentoEngine.iniciarComMultiplos(
//                 procBatalha.getId(),
//                 sessao.getId(),
//                 todosIds
//         );

//         // Store the battle ID in the procedure context so handlers can
//         // use InstanciaResolver's "batalha.*" fontes
//         ctxBatalha.getContexto().put("id_batalha", batalha.getId());

//         batalha.setStatus(BatalhaStatus.EM_ANDAMENTO);
//         batalhaRepo.save(batalha);

//         // Broadcast battle start to all session participants

//         broadcastBatalha(sessao.getId(), "BATALHA_INICIADA", Map.of(
//                 "idBatalha",          batalha.getId(),
//                 "participantesTime0", toParticipanteDTO(instanciasJogadores),
//                 "participantesTime1", toParticipanteDTO(instanciasInimigos),
//                 "estadoProcedimento", ProcedimentoContextoDTO.from(ctxBatalha)
//         ));

//         log.info("Batalha {} iniciada na sessão {} com {} jogadores e {} inimigos",
//                 batalha.getId(), req.idSessao(),
//                 instanciasJogadores.size(), instanciasInimigos.size());

//         return toDTO(batalha);
//     }

//     /**
//      * Master adds more instances to a running battle.
//      * Useful for ambushes, reinforcements, or NPC events mid-combat.
//      */
//     public void adicionarParticipante(Long idBatalha, Long idInstancia,
//                                       int lado, Long idMestre) {
//         Batalha batalha = batalhaRepo.findById(idBatalha).orElseThrow(() ->
//                 new EntityNotFoundException(Batalha.class, idBatalha)
//         );
//         autorizacao.exigirMestre(batalha.getSessao().getCampanha().getId(), idMestre);

//         EntidadeInstancia inst = instanciaRepo.findById(idInstancia).orElseThrow(() ->
//                 new EntityNotFoundException(EntidadeInstancia.class, idInstancia)
//         );
//         addParticipantes(batalha, List.of(inst), lado);

//         broadcastBatalha(batalha.getSessao().getId(), "PARTICIPANTE_ADICIONADO",
//                 Map.of("idBatalha", idBatalha, "idInstancia", idInstancia, "lado", lado));
//     }

//     /**
//      * Removes an instance from the battle (fled, captured, etc.)
//      * without ending the battle.
//      */
//     public void removerParticipante(Long idBatalha, Long idInstancia, Long idMestre) {
//         Batalha batalha = batalhaRepo.findById(idBatalha).orElseThrow(() ->
//                 new EntityNotFoundException(Batalha.class, idBatalha)
//         );
//         autorizacao.exigirMestre(
//                 batalha.getSessao().getCampanha().getId(), idMestre);

//         BatalhaParticipantesKey id = new BatalhaParticipantesKey(idBatalha, idInstancia);
//         batalhaInstanciaRepo.findById(id).ifPresent(bp -> {
//             bp.setAtivo(false);
//             batalhaInstanciaRepo.save(bp);
//         });

//         broadcastBatalha(batalha.getSessao().getId(), "PARTICIPANTE_REMOVIDO",
//                 Map.of("idBatalha", idBatalha, "idInstancia", idInstancia));
//     }

//     /**
//      * Master forcibly ends a battle (TPK, surrender, narrative resolution).
//      */
//     public void encerrarBatalha(Long idBatalha, String motivo, Long idMestre) {
//         Batalha batalha = batalhaRepo.findById(idBatalha).orElseThrow();
//         autorizacao.exigirMestre(
//                 batalha.getSessao().getCampanha().getId(), idMestre);

//         batalha.setStatus(BatalhaStatus.CONCLUIDA);
//         batalhaRepo.save(batalha);

//         broadcastBatalha(batalha.getSessao().getId(), "BATALHA_ENCERRADA",
//                 Map.of("idBatalha", idBatalha, "motivo", motivo));
//     }

//     // ── Internal helpers ──────────────────────────────────────────

//     private List<EntidadeInstancia> resolverInstancias(List<Long> ids,
//                                                        Long idCampanha,
//                                                        String contexto) {
//         if (ids == null || ids.isEmpty()) return List.of();

//         List<EntidadeInstancia> instancias = instanciaRepo.findAllById(ids);

//         // Verify all resolved — none missing
//         if (instancias.size() != ids.size()) {
//             Set<Long> encontrados = instancias.stream()
//                     .map(EntidadeInstancia::getId)
//                     .collect(Collectors.toSet());
//             List<Long> ausentes = ids.stream()
//                     .filter(id -> !encontrados.contains(id))
//                     .toList();
//             throw new RecursoNaoEncontradoException(
//                     "Instâncias não encontradas para " + contexto + ": " + ausentes);
//         }

//         // Verify all belong to this campaign
//         List<Long> foraDaCampanha = instancias.stream()
//                 .filter(i -> !i.getCampanha().getId().equals(idCampanha))
//                 .map(EntidadeInstancia::getId)
//                 .toList();

//         if (!foraDaCampanha.isEmpty()) {
//             throw new DeniedAcessException(
//                     "Instâncias não pertencem à campanha " + idCampanha +
//                             ": " + foraDaCampanha);
//         }

//         return instancias;
//     }

//     private void addParticipantes(Batalha batalha,
//                                   List<EntidadeInstancia> instancias,
//                                   int lado) {
//         for (int i = 0; i < instancias.size(); i++) {
//             BatalhaParticipantes bp = new BatalhaParticipantes();
//             bp.setId(new BatalhaParticipantesKey(
//                     batalha.getId(),
//                     instancias.get(i).getId()));
//             bp.setBatalha(batalha);
//             bp.setEntidadeInstancia(instancias.get(i));
//             bp.setLado(lado);
//             bp.setOrdemIniciativa(i); // will be overwritten by ORDENAR_PARTICIPANTES etapa
//             bp.setAtivo(true);
//             batalhaInstanciaRepo.save(bp);
//         }
//     }

//     private Sessao exigirSessaoAtiva(Long idSessao) {
//         Sessao sessao = sessaoRepo.findById(idSessao).orElseThrow(() ->
//                 new RecursoNaoEncontradoException("Sessão não encontrada: " + idSessao));
//         if (sessao.getStatus() != StatusSessao.ATIVA) {
//             throw new EstadoInvalidoException("Sessão " + idSessao + " não está ativa");
//         }
//         return sessao;
//     }

//     private void broadcastBatalha(Long idSessao, String tipo, Map<String, Object> payload) {
//         messagingTemplate.convertAndSend(
//                 "/topic/sessao/" + idSessao,
//                 (Object) Map.of("tipo", tipo, "payload", payload)
//         );
//     }

//     private List<Map<String, Object>> toParticipanteDTO(List<EntidadeInstancia> instancias) {
//         return instancias.stream().map(i -> Map.<String, Object>of(
//                 "idInstancia", i.getId(),
//                 "nome",        i.getNome(),
//                 "tipo",        i.getTipo()
//         )).toList();
//     }

//     private BatalhaDTO toDTO(Batalha b) {
//         return new BatalhaDTO(b.getId(), b.getStatus(), b.getRodadaAtual());
//     }
// }
