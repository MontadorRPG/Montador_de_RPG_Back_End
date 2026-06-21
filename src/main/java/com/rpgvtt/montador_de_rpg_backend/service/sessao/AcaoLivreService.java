package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.EfeitoAtivo;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.dto.mecanica.TesteLivreDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.mecanica.TesteLivreRequestDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.personagem.UsoItemDTO;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.PrimitivoExecutor;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEfeito;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine.ResultadoRolagem;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine.RolagemEngine;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine.VantagemTipo;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.personagem.PersonagemRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.EfeitoAtivoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.SistemaRepository;
import com.rpgvtt.montador_de_rpg_backend.service.CampanhaAutorizacao;
import com.rpgvtt.montador_de_rpg_backend.service.exceptions.EstadoInvalidoException;
import com.rpgvtt.montador_de_rpg_backend.service.exceptions.RecursoNaoEncontradoException;
import com.rpgvtt.montador_de_rpg_backend.service.mecanica.EfeitosAtivosService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AcaoLivreService {

    private final PersonagemRepository personagemRepo;
    private final EntidadeInstanciaRepository instanciaRepo;
    private final RolagemEngine rolagemEngine;
    private final PrimitivoExecutor primitivoExecutor;
    private final EfeitosAtivosService efeitoAtivosService;
    private final EfeitoAtivoRepository efeitosAtivosRepo;
    private final SessaoRepository sessaoRepo;
    private final CampanhaAutorizacao autorizacao;
    private final SimpMessagingTemplate messagingTemplate;
    private final JsonMapper mapper;
    private final SistemaRepository sistemaRepo;

    // ════════════════════════════════════════════════════════════
    // Free checks — Saves, skill checks, anything with a target number
    // ════════════════════════════════════════════════════════════

    /**
     * Rolls a die against an optional difficulty/target number.
     * Used for Mythic Bastionland Saves (VIG/CLA/SPI) or any ad-hoc check
     * the GM calls for without a formal entidade_efeito definition.
     */
    public TesteLivreDTO rolarTesteLivre(Long idPersonagem, Long idUsuario,
                                         TesteLivreRequestDTO req) {
        Personagem personagem = exigirPersonagem(idPersonagem);
        autorizacao.exigirMembro(personagem.getId(), idUsuario);
        EntidadeInstancia inst = exigirInstancia(personagem);

        int modificador = resolverModificador(inst, req.atributo(), personagem.getId());

        ResultadoRolagem rolo = rolagemEngine.executarComVantagem(
                RolagemEngine.simples(req.dado(), 1, false),
                VantagemTipo.valueOf(req.vantagem() != null ? req.vantagem() : "NORMAL")
        );

        int total = rolo.total() + modificador;
        Boolean sucesso = req.dificuldade() != null ? total >= req.dificuldade() : null;

        TesteLivreDTO dto = new TesteLivreDTO(
                req.atributo(), req.dado(), rolo.rolos(), modificador, total,
                req.dificuldade(), sucesso
        );

        broadcast(personagem, "TESTE_LIVRE", dto);
        return dto;
    }

    // ════════════════════════════════════════════════════════════
    // Item usage — potions, scrolls, anything consumable with effects
    // ════════════════════════════════════════════════════════════

    /**
     * Consumes an item and runs its declared effects.
     *
     * Item JSON is expected to optionally carry:
     * "efeitos": [
     *   { "primitivo": "CURA", "parametros": { "dado": "2d4", "bonus": 2 } },
     *   { "primitivo": "REMOVER_STATUS", "parametros": { "status": "envenenado" } }
     * ]
     *
     * If a parametro value is a dice expression under "dado", it's rolled
     * here and merged into the params as "valor" before the primitivo runs.
     */
    public UsoItemDTO usarItem(Long idPersonagem, Long idSistema, Long idUsuario,
                               String idItem, Long idInstanciaAlvo) {
        Personagem personagem = exigirPersonagem(idPersonagem);
        autorizacao.exigirAcessoPersonagem(personagem, idUsuario); // dono ou mestre
        EntidadeInstancia executor = exigirInstancia(personagem);

        Sistema sistema = sistemaRepo.findById(idSistema)
                .orElseThrow(() -> new EntityNotFoundException(Sistema.class, idSistema));

        EntidadeInstancia alvo = idInstanciaAlvo != null
                ? instanciaRepo.findById(idInstanciaAlvo).orElseThrow()
                : executor; // self-target by default (drinking your own potion)

        ObjectNode attrs = (ObjectNode) executor.getAtributosAtuais();
        ArrayNode inventario = (ArrayNode) attrs.path("inventario");

        ObjectNode item = encontrarItem(inventario, idItem);

        if (!"CONSUMIVEL".equals(item.path("tipo").asString())) {
            throw new EstadoInvalidoException("Item '" + idItem + "' não é consumível");
        }

        int quantidadeAtual = item.path("quantidade").asInt(1);
        if (quantidadeAtual <= 0) {
            throw new EstadoInvalidoException("Item sem unidades restantes");
        }

        // Run declared effects, rolling any dice expressions first
        JsonNode efeitosNode = item.path("efeitos");
        List<ResultadoEtapa> resultados = new ArrayList<>();

        if (efeitosNode.isArray()) {
            for (JsonNode efeitoNode : efeitosNode) {
                String nomePrimitivo = efeitoNode.path("primitivo").asString();
                Map<String, Object> params = new HashMap<>(
                        mapper.convertValue(efeitoNode.path("parametros"),
                                new TypeReference<Map<String, Object>>() {}));

                // Roll dice expression if present
                if (params.containsKey("dado")) {
                    String dado = (String) params.remove("dado");
                    int bonus = ((Number) params.getOrDefault("bonus", 0)).intValue();
                    ResultadoRolagem rolo = rolagemEngine.executar(
                            RolagemEngine.simples(dado, 1, false));
                    params.put("valor", rolo.total() + bonus);
                }

                resultados.add(primitivoExecutor.executarPrimitivoAvulso(
                        nomePrimitivo, sistema, executor, alvo, params));
            }
        }

        // Decrement quantity, remove item entirely if it reaches zero
        int novaQuantidade = quantidadeAtual - 1;
        if (novaQuantidade <= 0) {
            removerItemDoInventario(inventario, idItem);
        } else {
            item.put("quantidade", novaQuantidade);
        }

        executor.setAtributosAtuais(attrs);
        instanciaRepo.save(executor);
        if (!alvo.getId().equals(executor.getId())) {
            instanciaRepo.save(alvo); // primitivo may have mutated the target directly
        }

        UsoItemDTO dto = new UsoItemDTO(
                idItem, item.path("nome").asString(), alvo.getId(),
                novaQuantidade > 0, resultados
        );

        broadcast(personagem, "ITEM_USADO", dto);
        log.info("Personagem {} usou item {} ({}), {} unidades restantes",
                idPersonagem, idItem, item.path("nome").asString(), novaQuantidade);

        return dto;
    }

    // ════════════════════════════════════════════════════════════
    // Rest — restore resources between scenes
    // ════════════════════════════════════════════════════════════

    /**
     * Restores resources according to the rest type.
     * Specific restoration rules are read from sistema.configsGeral
     * so different systems can define different rest semantics
     * without new code — only "CURTO" vs "LONGO" mapping is hardcoded
     * as a concept, not the amounts.
     *
     * configsGeral example:
     * {
     *   "descanso_longo": { "restaura_hp_total": true, "restaura_slots": true,
     *                        "restaura_recursos": ["acao_disponivel", ...] },
     *   "descanso_curto": { "restaura_hp_percentual": 0.25, "restaura_slots": false }
     * }
     */
    public DescansoDTO descansar(Long idPersonagem, Long idUsuario, String tipo) {
        Personagem personagem = exigirPersonagem(idPersonagem);
        autorizacao.exigirAcessoPersonagem(personagem, idUsuario);
        EntidadeInstancia inst = exigirInstancia(personagem);

        Sistema sistema = personagem.getCampanha().getSistema();
        JsonNode configDescanso = sistema.getConfiguracao()
                .path("descanso_" + tipo.toLowerCase()); // "descanso_longo" | "descanso_curto"

        ObjectNode attrs = (ObjectNode) inst.getAtributosAtuais();
        Map<String, Object> restaurado = new LinkedHashMap<>();

        if (configDescanso.path("restaura_hp_total").asBoolean(false)) {
            int hpMax = attrs.path("hp_max").asInt(0);
            attrs.put("hp", hpMax);
            restaurado.put("hp", hpMax);
        } else if (configDescanso.has("restaura_hp_percentual")) {
            double pct   = configDescanso.path("restaura_hp_percentual").asDouble(0);
            int hpAtual  = attrs.path("hp").asInt(0);
            int hpMax    = attrs.path("hp_max").asInt(0);
            int restauro = (int) Math.round(hpMax * pct);
            int novoHp   = Math.min(hpMax, hpAtual + restauro);
            attrs.put("hp", novoHp);
            restaurado.put("hp", novoHp);
        }

        if (configDescanso.path("restaura_slots").asBoolean(false)
                && attrs.has("slots_magia_max")) {
            attrs.set("slots_magia", attrs.path("slots_magia_max").deepCopy());
            restaurado.put("slots_magia", attrs.path("slots_magia_max"));
        }

        JsonNode recursosNode = configDescanso.path("restaura_recursos");
        if (recursosNode.isArray()) {
            recursosNode.forEach(n -> {
                attrs.put(n.asString(), true);
                restaurado.put(n.asString(), true);
            });
        }

        inst.setAtributosAtuais(attrs);
        instanciaRepo.save(inst);

        // Clear expired/non-persistent effects after a long rest
        if ("LONGO".equalsIgnoreCase(tipo)) {
            limparEfeitosTemporarios(inst.getId());
        }

        DescansoDTO dto = new DescansoDTO(tipo, restaurado);
        broadcast(personagem, "DESCANSO_REALIZADO", dto);
        return dto;
    }

    private void limparEfeitosTemporarios(Long idInstancia) {
        List<EfeitoAtivo> ativos = efeitosAtivosRepo.findByEntidadeInstanciaIdAndAtivo(idInstancia, Boolean.TRUE);
        for (EfeitoAtivo ef : ativos) {
            Map<String, Object> efParametros = mapper.convertValue(ef.getParametros(), new TypeReference<>() {});
            boolean persisteDescanso = efParametros != null
                    && Boolean.TRUE.equals(efParametros.get("persiste_descanso"));
            if (!persisteDescanso) {
                efeitosAtivosRepo.delete(ef);
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // Manual effects — GM applies a status/condition outside combat
    // ════════════════════════════════════════════════════════════

    /**
     * Master applies an arbitrary effect to an instance outside battle —
     * a trap's poison, a shrine's blessing, narrative consequences.
     *
     * Expiration is time-based here (duracaoSegundos), not round-based,
     * since there's no Batalha ticking rounds.
     */
    public EfeitoManualDTO aplicarEfeitoManual(Long idCampanha, Long idUsuarioMestre,
                                               AplicarEfeitoManualRequest req) {
        autorizacao.exigirMestre(idCampanha, idUsuarioMestre);

        EntidadeInstancia inst = instanciaRepo.findById(req.idInstancia()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Instância não encontrada: " + req.idInstancia()));

        EfeitoAtivo efeito = new EfeitoAtivo();
        efeito.setEntidadeInstancia(inst);
        efeito.setParametros(mapper.valueToTree(req.parametros()));
        efeito.setMomento("PASSIVO");

        if (req.usos() != null) {
            efeito.setUsosRestantes(req.usos());
        }

        efeitosAtivosRepo.save(efeito);

        EfeitoManualDTO dto = new EfeitoManualDTO(
                efeito.getId(), req.idInstancia(),
                req.parametros(), efeito.getExpiraEm()
        );

        broadcastSessao(idCampanha, "EFEITO_MANUAL_APLICADO", dto);
        log.info("Mestre {} aplicou efeito manual na instância {}: {}",
                idUsuarioMestre, req.idInstancia(), req.parametros());

        return dto;
    }

    /**
     * Master removes an effect manually (cured early, narrative resolution).
     */
    public void removerEfeitoManual(Long idCampanha, Long idUsuarioMestre, Long idEfeitoAtivo) {
        autorizacao.exigirMestre(idCampanha, idUsuarioMestre);

        EfeitoAtivo efeito = efeitosAtivosRepo.findById(idEfeitoAtivo).orElseThrow(() ->
                new RecursoNaoEncontradoException("Efeito ativo não encontrado: " + idEfeitoAtivo));

        efeitosAtivosRepo.delete(efeito);
        broadcastSessao(idCampanha, "EFEITO_MANUAL_REMOVIDO",
                Map.of("idEfeitoAtivo", idEfeitoAtivo));
    }

    /**
     * Processes timestamp-based effect expiration for one instance.
     * Call this when relevant (GM clicks "advance time", or lazily on
     * each free-action endpoint call for that instance).
     */
    public List<ResultadoEfeito> processarEfeitosPassivos(Long idInstancia) {
        return efeitoAtivosService.processarForaDeCombate(idInstancia);
    }

    // ── Internal helpers ────────────────────────────────────────

    private Personagem exigirPersonagem(Long idPersonagem) {
        return personagemRepo.findById(idPersonagem).orElseThrow(() ->
                new RecursoNaoEncontradoException("Personagem não encontrado: " + idPersonagem));
    }

    private EntidadeInstancia exigirInstancia(Personagem personagem) {
        return instanciaRepo.findById(personagem.getId()).orElseThrow();
    }

    private int resolverModificador(EntidadeInstancia inst, String atributo, Long idCampanha) {
        JsonNode val = inst.getAtributosAtuais().get(atributo);
        if (val == null) return 0;
        int raw = val.asInt(10);

        // Mythic Bastionland uses direct attribute value as the target,
        // not a D&D-style modifier — but keep this configurable via sistema
        return raw; // adjust per sistema.configsGeral.formula_modificador if needed
    }

    private ObjectNode encontrarItem(ArrayNode inventario, String idItem) {
        for (JsonNode node : inventario) {
            if (idItem.equals(node.path("id").asString())) return (ObjectNode) node;
        }
        throw new RecursoNaoEncontradoException("Item não encontrado: " + idItem);
    }

    private void removerItemDoInventario(ArrayNode inventario, String idItem) {
        for (int i = 0; i < inventario.size(); i++) {
            if (idItem.equals(inventario.get(i).path("id").asString())) {
                inventario.remove(i);
                return;
            }
        }
    }

    private void broadcast(Personagem personagem, String tipo, Object payload) {
        sessaoRepo.findAtivaByCampanhaId(personagem.getCampanha().getId()).ifPresent(sessao ->
                messagingTemplate.convertAndSend(
                        "/topic/sessao/" + sessao.getId() + "/acoes-livres",
                        (Object) Map.of("tipo", tipo, "idPersonagem", personagem.getId(), "payload", payload)
                )
        );
    }

    private void broadcastSessao(Long idCampanha, String tipo, Object payload) {
        sessaoRepo.findAtivaByCampanhaId(idCampanha).ifPresent(sessao ->
                messagingTemplate.convertAndSend(
                        "/topic/sessao/" + sessao.getId() + "/acoes-livres",
                        (Object) Map.of("tipo", tipo, "payload", payload)
                )
        );
    }
}
