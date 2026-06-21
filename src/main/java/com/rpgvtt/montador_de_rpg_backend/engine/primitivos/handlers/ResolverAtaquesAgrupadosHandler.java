package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.engine.components.ItemResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.*;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.ColetaParcialUtil;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine.Rolagem;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine.RolagemEngine;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ResolverAtaquesAgrupadosHandler implements EtapaHandler {

    private final RolagemEngine rolagemEngine;
    private final ItemResolver itemResolver;
    private final EntidadeInstanciaRepository instanciaRepo;

    @Override
    public String tipoEtapa() { return "RESOLVER_ATAQUES_AGRUPADOS"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();

        String declaracoesFonte  = exigirTexto(params, "declaracoes_fonte", etapa);
        JsonNode origemDadoAtaque = exigirNo(params, "dado_ataque", etapa);
        JsonNode origemReducao    = params.get("reducao_dano"); // optional

        int limiarGambit        = params.path("limiar_gambit").asInt(4);
        String trackDano        = params.path("track_dano").asString("guard");
        String overflowPara     = params.path("overflow_para").asString(null);
        String salvarResultados = params.path("salvar_resultados_em").asString(null);
        String salvarGambits    = params.path("salvar_gambits_em").asString(null);
        String salvarFeridosEm  = params.path("salvar_alvos_feridos_em").asString(null);

        Map<Long, Long> declaracoes = lerDeclaracoes(declaracoesFonte, ctx, etapa);

        Map<Long, List<Long>> atacantesPorAlvo = new LinkedHashMap<>();
        for (var e : declaracoes.entrySet()) {
            if (e.getValue() == null) continue; // passed
            atacantesPorAlvo.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }

        Map<Long, Object> resultadosPorAlvo = new LinkedHashMap<>();
        Map<Long, Object> gambits = new LinkedHashMap<>();
        List<Long> alvosFeridos = new ArrayList<>();

        for (var grupo : atacantesPorAlvo.entrySet()) {
            Long idAlvo = grupo.getKey();
            List<Long> atacantes = grupo.getValue();

            // ── Each attacker rolls with THEIR OWN weapon die ──────────────
            Map<Long, Integer> rolosPorAtacante = new LinkedHashMap<>();
            for (Long idAtacante : atacantes) {
                Rolagem rolagem = resolverDadoAtaqueDoAtacante(origemDadoAtaque, idAtacante, etapa);
                int rolo = rolagemEngine.executar(rolagem).total();
                rolosPorAtacante.put(idAtacante, rolo);
            }

            var maxEntry = rolosPorAtacante.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElseThrow();
            int danoBruto = maxEntry.getValue();

            // ── Reduction comes from the TARGET's own equipped shield/armor ───
            int reducao = resolverReducaoDoAlvo(origemReducao, idAlvo);
            int danoFinal = Math.max(0, danoBruto - reducao);

            EntidadeInstancia alvo = instanciaRepo.findById(idAlvo)
                    .orElseThrow(() -> new EntityNotFoundException(EntidadeInstancia.class, idAlvo));
            int danoSobrando = aplicarDano(alvo, danoFinal, trackDano, overflowPara);

            resultadosPorAlvo.put(idAlvo, Map.of(
                    "rolosPorAtacante", rolosPorAtacante,
                    "idVencedor", maxEntry.getKey(),
                    "danoBruto", danoBruto,
                    "reducao", reducao,
                    "danoAplicado", danoFinal,
                    "guardRestante", danoSobrando
            ));

            if (danoSobrando < 0 && salvarFeridosEm != null) alvosFeridos.add(idAlvo);

            // Gambits use the RAW roll — reduction only affects damage, not gambit eligibility
            for (var rolo : rolosPorAtacante.entrySet()) {
                if (rolo.getValue() > limiarGambit) {
                    gambits.put(rolo.getKey(), Map.of("valorRolado", rolo.getValue(), "idAlvoOriginal", idAlvo));
                }
            }
        }

        if (salvarResultados != null) ctx.getContexto().put(salvarResultados, resultadosPorAlvo);
        if (salvarGambits != null) {
            ctx.getContexto().put(salvarGambits, gambits);
            ctx.getContexto().put(salvarGambits + "_ids", new ArrayList<>(gambits.keySet()));
        }
        if (salvarFeridosEm != null) ctx.getContexto().put(salvarFeridosEm, alvosFeridos);

        return ResultadoEtapa.concluida(Map.of(
                "resultadosPorAlvo", resultadosPorAlvo, "gambitsDisponiveis", gambits.size()));
    }

    // ════════════════════════════════════════════════════════════
    // Weapon resolution — called ONCE PER ATTACKER, never cached/shared.
    // ════════════════════════════════════════════════════════════

    /**
     * Resolves the die type a SPECIFIC attacker rolls with. This is the exact
     * piece that was reported broken, so it's deliberately inlined here — no
     * shared generic resolver, no JsonNode cloning — so it can be verified by
     * direct inspection: idAtacante always identifies whose weapon gets read.
     */
    private Rolagem resolverDadoAtaqueDoAtacante(JsonNode origemDadoAtaque, Long idAtacante, EtapaExecutavel etapa) {
        if (origemDadoAtaque.isString()) {
            return new Rolagem(origemDadoAtaque.asString(), 1, false); // same literal die for everyone (e.g. unarmed default)
        }

        String fonte = origemDadoAtaque.path("fonte").asString("ausente");
        String chave = origemDadoAtaque.path("chave").asString(null);

        return switch (fonte) {

            case "item_equipado" -> {
                String slot = origemDadoAtaque.path("slot").asString(null);
                if (slot == null || chave == null) {
                    throw new JsonFieldNotFoundException("slot/chave", etapa.getNome());
                }
                // Reads the weapon equipped by idAtacante — never the active instance,
                // never the target. idAtacante is whoever is rolling THIS attack.
                JsonNode dadoAtaque = itemResolver.atributoDoEquipado(idAtacante, slot, "dado_ataque");
                JsonNode dadoQtd = itemResolver.atributoDoEquipado(idAtacante, slot, "dado_qtd");
                if (dadoAtaque == null || dadoQtd == null) {
                    throw new IllegalStateException(
                            "Attacker " + idAtacante + " has nothing equipped in slot '" + slot +
                                    "' (or the equipped item lacks attribute '" + chave + "')");
                }
                yield new Rolagem(dadoAtaque.asString(), dadoQtd.asInt(), false);
            }

            case "atributo" -> {
                // Innate weapon — e.g. a goblin's bite/claw stored directly on its own atributosAtuais
                EntidadeInstancia atacante = instanciaRepo.findById(idAtacante)
                        .orElseThrow(() -> new EntityNotFoundException(EntidadeInstancia.class, idAtacante));
                JsonNode valor = atacante.getAtributosAtuais().get(chave);
                if (valor == null) {
                    throw new IllegalStateException("Attacker " + idAtacante + " has no attribute '" + chave + "'");
                }
                yield new Rolagem(valor.asString(), 1, false);
            }

            default -> throw new IllegalArgumentException("Unknown dado_ataque fonte: '" + fonte + "'");
        };
    }

    // ════════════════════════════════════════════════════════════
    // Armor/shield resolution — called ONCE PER TARGET, reading the target's own gear.
    // ════════════════════════════════════════════════════════════

    /**
     * Resolves damage reduction from whatever the TARGET (the victim) has
     * equipped. Returns 0 when the target has nothing relevant equipped —
     * that's a normal case (not every combatant carries a shield), not an error.
     */
    private int resolverReducaoDoAlvo(JsonNode origemReducao, Long idAlvo) {
        if (origemReducao == null || origemReducao.isMissingNode() || origemReducao.isNull()) return 0;

        String fonte = origemReducao.path("fonte").asString("ausente");
        String chave = origemReducao.path("chave").asString(null);

        return switch (fonte) {
            case "ausente" -> 0;

            case "item_equipado" -> {
                String slot = origemReducao.path("slot").asString(null);
                if (slot == null || chave == null) yield 0;
                JsonNode valorItem = itemResolver.atributoDoEquipado(idAlvo, slot, chave);
                yield valorItem != null ? valorItem.asInt(0) : 0;
            }

            case "atributo" -> {
                if (chave == null) yield 0;
                EntidadeInstancia alvo = instanciaRepo.findById(idAlvo)
                        .orElseThrow(() -> new EntityNotFoundException(EntidadeInstancia.class, idAlvo));
                JsonNode valor = alvo.getAtributosAtuais().get(chave);
                yield valor != null ? valor.asInt(0) : 0;
            }

            case "fixo" -> origemReducao.path("chave").asInt(0);

            default -> throw new IllegalArgumentException("Unknown reducao_dano fonte: '" + fonte + "'");
        };
    }

    // ════════════════════════════════════════════════════════════

    private int aplicarDano(EntidadeInstancia alvo, int dano, String track, String overflow) {
        ObjectNode attrs = (ObjectNode) alvo.getAtributosAtuais();
        int atual = attrs.path(track).asInt(0);
        int restante = atual - dano;

        if (restante >= 0) {
            attrs.put(track, restante);
        } else {
            attrs.put(track, 0);
            if (overflow != null) {
                int overflowAtual = attrs.path(overflow).asInt(0);
                attrs.put(overflow, Math.max(0, overflowAtual + restante));
            }
        }
        alvo.setAtributosAtuais(attrs);
        instanciaRepo.save(alvo);
        return restante;
    }

    private Map<Long, Long> lerDeclaracoes(String fonte, ExecucaoContexto ctx, EtapaExecutavel etapa) {
        String chave = fonte.startsWith("contexto.") ? fonte.substring("contexto.".length()) : fonte;
        Object raw = ctx.getContexto().get(chave, Object.class)
                .orElseThrow(() -> new JsonFieldNotFoundException(chave, etapa.getNome()));
        if (!(raw instanceof Map<?, ?> rawMap)) {
            throw new IllegalStateException("Expected a Map at contexto['" + chave + "'], found " + raw.getClass());
        }
        Map<Long, Long> out = new LinkedHashMap<>();
        for (var e : rawMap.entrySet()) {
            Long idAtacante = ColetaParcialUtil.comoLong(e.getKey());
            Long idAlvo = ColetaParcialUtil.comoLong(extrairIdAlvo(e.getValue()));
            if (idAtacante != null) out.put(idAtacante, idAlvo);
        }
        return out;
    }

    private Object extrairIdAlvo(Object declaracao) {
        if (declaracao instanceof Map<?, ?> m) return m.get("id_alvo");
        return declaracao;
    }

    private JsonNode exigirNo(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.get(chave);
        if (v == null) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v;
    }

    private String exigirTexto(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v.asString();
    }
}