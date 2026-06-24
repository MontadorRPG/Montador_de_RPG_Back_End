package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusSessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.AtributoAlteradoDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.ConviteDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.EntradaSessaoDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.SessaoDTO;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.ProcedimentoEngine;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaUsuarioRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.personagem.PersonagemRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.ProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.usuario.UsuarioRepository;
import com.rpgvtt.montador_de_rpg_backend.service.CampanhaAutorizacao;
import com.rpgvtt.montador_de_rpg_backend.service.exceptions.DeniedAcessException;
import com.rpgvtt.montador_de_rpg_backend.service.exceptions.EstadoInvalidoException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Transactional
@Slf4j
@Service
public class SessaoService {

        private final SessaoRepository sessaoRepo;
        private final CampanhaRepository campanhaRepo;
        private final PersonagemRepository personagemRepo;
        private final EntidadeInstanciaRepository instanciaRepo;
        private final CampanhaUsuarioRepository campUsuarioRepo;
        private final SessaoParticipanteCache participanteCache; // in-memory room roster
        private final ProcedimentoEngine procedimentoEngine;
        private final CampanhaAutorizacao autorizacao;
        private final SimpMessagingTemplate messagingTemplate;
        private final ProcedimentoRepository procedimentoRepo;
        private final JsonMapper mapper;
        private final UsuarioRepository usuarioRepository;

        // ── Session lifecycle ─────────────────────────────────────────

        /**
         * Master opens a session for a campaign.
         * Creates the sessoes record, transitions campaign to IN_SESSION state,
         * starts the root procedure, and broadcasts the session open event.
         *
         * Only one active session per campaign is allowed.
         */
        public SessaoDTO iniciarSessao(Long idCampanha, Long idUsuarioMestre) {
        autorizacao.exigirMestre(idCampanha, idUsuarioMestre);

        Campanha campanha = campanhaRepo.findById(idCampanha).orElseThrow();

        // Guard: no double sessions
        sessaoRepo.findAtivaByCampanhaId(idCampanha).ifPresent(s -> {
                throw new EstadoInvalidoException("Já existe uma sessão ativa para esta campanha: " + s.getId());
        });

        // Create the session record
        Sessao sessao = new Sessao();
        sessao.setCampanha(campanha);
        sessao.setStatus(StatusSessao.ATIVA);
        sessao.setDataInicio(LocalDateTime.now());
        sessao.setOrdem(proximaOrdemSessao(idCampanha));
        sessaoRepo.save(sessao);

        // Start the root session procedure (SESSAO_ATIVA type)
        // This procedure lives until the session ends — all combat procedures
        // are children of this root
        //        Procedimento procRaiz = procedimentoRepo
        //                .findByTipoAndIdSistema("SESSAO_ATIVA", campanha.getSistema().getId())
        //                .orElseThrow(() -> new ConfiguracaoException(
        //                        "Procedimento SESSAO_ATIVA não encontrado para o sistema " +
        //                                campanha.getSistema().getNome()));
        //
        //        procedimentoEngine.iniciarSemInstancia(
        //                procRaiz.getIdProcedimento(),
        //                sessao.getIdSessao()
        //        );

        // Initialize the in-memory participant roster with the master
        participanteCache.inicializar(sessao.getId(), idUsuarioMestre);

        // Broadcast session open to all campaign members
        broadcastSessao(sessao, "SESSAO_INICIADA", Map.of(
                "idSessao",    sessao.getId(),
                "idCampanha",  idCampanha,
                "nomeMestre",  campanha.getNome()
        ));

        log.info("Sessão {} iniciada pelo mestre {} na campanha {}",
                sessao.getId(), idUsuarioMestre, idCampanha);

        return toDTO(sessao);
        }

        /**
         * Master ends the session.
         * Marks all active battles as ENCERRADA, concludes the root procedure,
         * and broadcasts the closure.
         */
        public void encerrarSessao(Long idSessao, Long idUsuarioMestre) {
        Sessao sessao = exigirSessaoAtiva(idSessao);
        autorizacao.exigirMestre(sessao.getCampanha().getId(), idUsuarioMestre);

        sessao.setStatus(StatusSessao.ENCERRADA);
        sessao.setDataFim(LocalDateTime.now());
        sessaoRepo.save(sessao);

        participanteCache.limpar(idSessao);

        broadcastSessao(sessao, "SESSAO_ENCERRADA", Map.of("idSessao", idSessao));
        log.info("Sessão {} encerrada", idSessao);
        }

        // ── Player joining ────────────────────────────────────────────

        /**
         * Player requests to join the session room.
         *
         * Two paths:
         *  (a) Has a valid invite token → accepted immediately
         *  (b) No token → auto-join if they have a living personagem in the campaign
         *
         * On success, broadcasts the updated participant list to all room members.
         */
        public EntradaSessaoDTO entrarNaSessao(Long idSessao, Long idUsuario, String tokenConvite) {
        Sessao sessao = exigirSessaoAtiva(idSessao);
        Long idCampanha = sessao.getCampanha().getId();

        // Must be a campaign member regardless of path
        autorizacao.exigirMembro(idCampanha, idUsuario);

        // Masters always enter without restrictions
        boolean isMestre = autorizacao.isMestre(idCampanha, idUsuario);

        EntidadeInstancia instanciaPersonagem = null;

        if (!isMestre) {
                if (tokenConvite != null && !tokenConvite.isBlank()) {
                // Path A: validate invite token
                validarTokenConvite(idSessao, idUsuario, tokenConvite);
                } else {
                // Path B: auto-join — must have alive personagem
                Personagem personagem = autorizacao.exigirPersonagemVivo(idCampanha, idUsuario);
                instanciaPersonagem = instanciaRepo
                        .findById(personagem.getId())
                        .orElseThrow();
                }
        }

        participanteCache.adicionar(idSessao, new ParticipanteSessao(
                idUsuario, isMestre, instanciaPersonagem));

        // Broadcast updated roster to all room members
        broadcastParticipantes(sessao);

        return new EntradaSessaoDTO(
                idSessao,
                idUsuario,
                isMestre,
                instanciaPersonagem != null ? instanciaPersonagem.getId() : null
        );
        }

        /**
         * Player or master leaves the room (disconnects or explicitly leaves).
         * Does not end the session — master leaving pauses it.
         */
        public void sairDaSessao(Long idSessao, Long idUsuario) {
        Sessao sessao = sessaoRepo.findById(idSessao)
                .orElseThrow(() -> new EntityNotFoundException(Sessao.class, idSessao));

        // Prestar atenção se na restauração da sessão, o participanteCache volta também
        boolean eraMestre = participanteCache.isMestre(idSessao, idUsuario);
        participanteCache.remover(idSessao, idUsuario);

        if (eraMestre && sessao.getStatus() == StatusSessao.ATIVA) {
                // Master left — pause session so players can't act without GM
                sessao.setStatus(StatusSessao.PAUSADA);
                sessaoRepo.save(sessao);
                broadcastSessao(sessao, "SESSAO_PAUSADA",
                        Map.of("motivo", "Mestre desconectado"));
        }

        broadcastParticipantes(sessao);
        }

        /**
         * Remove a user from all sessions they are currently connected to
         * (used on websocket disconnect where session id is not known).
         */
        public void sairDaSessao(Long idUsuario) {
                // copy to avoid concurrent modification
                java.util.Set<Long> salas = participanteCache.listarIds();
                for (Long idSessao : salas) {
                        Sessao sessao = sessaoRepo.findById(idSessao).orElse(null);
                        boolean eraMestre = participanteCache.isMestre(idSessao, idUsuario);
                        participanteCache.remover(idSessao, idUsuario);

                        if (sessao != null) {
                                if (eraMestre && sessao.getStatus() == StatusSessao.ATIVA) {
                                        sessao.setStatus(StatusSessao.PAUSADA);
                                        sessaoRepo.save(sessao);
                                        broadcastSessao(sessao, "SESSAO_PAUSADA",
                                                        Map.of("motivo", "Mestre desconectado"));
                                }
                                broadcastParticipantes(sessao);
                        }
                }
        }

        // ── Invite token ──────────────────────────────────────────────

        /**
         * Master generates a single-use invite token for a specific user.
         * Token expires in 10 minutes.
         */
        public ConviteDTO gerarConvite(Long idSessao, Long idUsuarioConvidado, Long idMestre) {
        Sessao sessao = exigirSessaoAtiva(idSessao);
        autorizacao.exigirMestre(sessao.getCampanha().getId(), idMestre);

        String token = UUID.randomUUID().toString();
        participanteCache.registrarConvite(idSessao, idUsuarioConvidado, token,
                LocalDateTime.now().plusMinutes(10));

        // Send invite notification via WebSocket to the specific user
        messagingTemplate.convertAndSendToUser(
                idUsuarioConvidado.toString(),
                "/queue/convite",
                Map.of(
                        "idSessao",  idSessao,
                        "token",     token,
                        "expiraEm",  LocalDateTime.now().plusMinutes(10)
                )
        );

        return new ConviteDTO(idSessao, idUsuarioConvidado, token);
        }

        public String buscarApelidoDoUsuario(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .map(Usuario::getApelido)
                .orElse("Jogador " + idUsuario);
        }


        // ── Attribute editing ─────────────────────────────────────────

        /**
         * Master can change any attribute on any instance in the session.
         * Used for manual corrections, narrative events, or system effects
         * the engine doesn't cover.
         */
        public AtributoAlteradoDTO alterarAtributoInstancia(
                Long idSessao, Long idInstancia,
                String atributo, Object novoValor,
                Long idMestre) {

        Sessao sessao = exigirSessaoAtiva(idSessao);
        autorizacao.exigirMestre(sessao.getCampanha().getId(), idMestre);

        EntidadeInstancia inst = instanciaRepo.findById(idInstancia).orElseThrow(() ->
                new EntityNotFoundException(EntidadeInstancia.class, idInstancia));

        ObjectNode atributos = (ObjectNode) inst.getAtributosAtuais();

        JsonNode valorAnterior = atributos.get(atributo);
        atributos.set(atributo, mapper.valueToTree(novoValor));

        inst.setAtributosAtuais(atributos);

        instanciaRepo.save(inst);

        // Broadcast the change so all clients update their UI immediately
        broadcastAtributo(sessao, idInstancia, atributo, valorAnterior, novoValor);

        log.info("Mestre {} alterou {}.{}: {} → {} na sessão {}",
                idMestre, idInstancia, atributo, valorAnterior, novoValor, idSessao);

        return new AtributoAlteradoDTO(idInstancia, atributo, valorAnterior, novoValor);
        }

        /**
         * Player can change their OWN character's non-combat attributes
         * (notes, appearance, inventory descriptions — things the engine doesn't control).
         * Combat stats (hp, ca, resources) are engine-only — rejected here.
         */
        public AtributoAlteradoDTO alterarAtributoPersonagem(
                Long idSessao, Long idInstancia,
                String atributo, Object novoValor,
                Long idUsuario) {

        Sessao sessao = exigirSessaoAtiva(idSessao);
        Long idCampanha = sessao.getCampanha().getId();
        autorizacao.exigirMembro(idCampanha, idUsuario);

        // Verify this instance belongs to this player
        Personagem personagem = personagemRepo
                .findByInstanciaIdAndUsuarioId(idInstancia, idUsuario)
                .orElseThrow(() -> new DeniedAcessException(
                        "Instância " + idInstancia + " não pertence ao usuário " + idUsuario));

        // Block combat-controlled attributes — engine owns these
        if (ATRIBUTOS_PROTEGIDOS.contains(atributo)) { // ???
                throw new DeniedAcessException(
                        "Atributo '" + atributo + "' é controlado pelo engine de combate");
        }

        Object valorAnterior = personagem.getInstancia().getAtributosAtuais().get(atributo);

        ObjectNode atributos = (ObjectNode) personagem.getInstancia().getAtributosAtuais();
        atributos.set(atributo, mapper.valueToTree(novoValor));
        personagem.getInstancia().setAtributosAtuais(atributos);

        instanciaRepo.save(personagem.getInstancia());

        broadcastAtributo(sessao, idInstancia, atributo, valorAnterior, novoValor);

        return new AtributoAlteradoDTO(idInstancia, atributo, valorAnterior, novoValor);
        }

        // Attributes the engine controls exclusively — players cannot edit these
        private static final Set<String> ATRIBUTOS_PROTEGIDOS = Set.of(
                "hp", "hp_max", "ca", "velocidade",
                "acao_disponivel", "acao_bonus_disponivel", "reacao_disponivel",
                "status_ativos", "resistencias", "imunidades", "vulnerabilidades"
        );

        public Long resolverInstanciaDoJogador(Long idSessao, Long idUsuario) {

        Campanha campanha = campanhaRepo.findBySessoesId(idSessao).orElseThrow(
                () -> new EntityNotFoundException(Campanha.class, idSessao)
        );
        Personagem personagem = personagemRepo.findAtivoByCampanhaIdAndUsuarioId(campanha.getId(), idUsuario).orElseThrow(
                () -> new EntityNotFoundException(Personagem.class, campanha.getId())
        );

        // EntidadeInstancia inst = instanciaRepo.findByPersonagemId(personagem.getId()).orElseThrow(
        //         () -> new EntityNotFoundException(EntidadeInstancia.class, personagem.getId())
        // );

        EntidadeInstancia inst = personagem.getInstancia();
        if (inst == null) throw new EntityNotFoundException(EntidadeInstancia.class, personagem.getId());

        return inst.getId();
        }


        // ── Internal helpers ──────────────────────────────────────────

        private Sessao exigirSessaoAtiva(Long idSessao) {
        Sessao sessao = sessaoRepo.findById(idSessao).orElseThrow(() ->
                new EntityNotFoundException(Sessao.class, idSessao));
        if (sessao.getStatus() != StatusSessao.ATIVA) {
                throw new EstadoInvalidoException(
                        "Sessão " + idSessao + " não está ativa (status: " + sessao.getStatus() + ")");
        }
        return sessao;
        }

        private int proximaOrdemSessao(Long idCampanha) {
        return sessaoRepo.countByCampanhaId(idCampanha) + 1;
        }

        private void validarTokenConvite(Long idSessao, Long idUsuario, String token) {
        participanteCache.consumirConvite(idSessao, idUsuario, token)
                .orElseThrow(() -> new DeniedAcessException(
                        "Token de convite inválido ou expirado"));
        }

        private void broadcastSessao(Sessao sessao, String tipo, Map<String, Object> payload) {
        messagingTemplate.convertAndSend(
                "/topic/campanha/" + sessao.getCampanha().getId(),
                (Object) Map.of("tipo", tipo, "payload", payload)
        );
        }

        private void broadcastParticipantes(Sessao sessao) {
        messagingTemplate.convertAndSend(
                "/topic/sessao/" + sessao.getId() + "/participantes",
                participanteCache.listar(sessao.getId())
        );
        }

        private void broadcastAtributo(Sessao sessao, Long idInstancia,
                                        String atributo, Object antes, Object depois) {
        messagingTemplate.convertAndSend(
                "/topic/sessao/" + sessao.getId() + "/atributos",
                (Object) Map.of(
                        "tipo",        "ATRIBUTO_ALTERADO",
                        "idInstancia", idInstancia,
                        "atributo",    atributo,
                        "antes",       antes,
                        "depois",      depois
                )
        );
        }

        private SessaoDTO toDTO(Sessao s) {
        return new SessaoDTO(s.getId(), s.getStatus(),
                s.getDataInicio(), s.getCampanha().getId());
        }


}