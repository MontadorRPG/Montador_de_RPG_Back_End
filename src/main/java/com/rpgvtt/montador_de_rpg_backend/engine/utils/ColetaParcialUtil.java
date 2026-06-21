package com.rpgvtt.montador_de_rpg_backend.engine.utils;

import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lógica compartilhada de "coletar respostas até o grupo estar completo",
 * usada por qualquer handler que aguarde múltiplos participantes antes de
 * prosseguir (COLETAR_DECLARACOES_PARALELAS, PROCESSAR_GAMBITS).
 *
 * Convenção de chaves no contexto:
 *   "_partial_<salvarEm>" -> Map<Long, Object> das respostas já recebidas
 *   "respondente_id"      -> id de quem respondeu nesta chamada (setado por responder())
 *   "<salvarEm>_entrada"  -> valor enviado pelo respondente nesta chamada
 */
public final class ColetaParcialUtil {

    private ColetaParcialUtil() {}

    @SuppressWarnings("unchecked")
    public static Map<Long, Object> carregarParcial(ExecucaoContexto ctx, String salvarEm) {
        return (Map<Long, Object>) ctx.getContexto()
                .get("_partial_" + salvarEm, Object.class)
                .orElseGet(LinkedHashMap::new);
    }

    public static Map<Long, Object> registrarResposta(ExecucaoContexto ctx, String salvarEm,
                                                      Map<Long, Object> parcial,
                                                      List<Long> idsEsperados) {
        Long respondente = ctx.getContexto().getLong("respondente_id").orElse(null);
        Object entrada    = ctx.getContexto().get(salvarEm + "_entrada", Object.class).orElse(null);

        if (respondente != null && idsEsperados.contains(respondente) && entrada != null) {
            parcial.put(respondente, entrada);
            ctx.getContexto().put("_partial_" + salvarEm, parcial);
            ctx.getContexto().remove("respondente_id");
            ctx.getContexto().remove(salvarEm + "_entrada");
        }
        return parcial;
    }

    public static List<Long> pendentes(List<Long> idsEsperados, Map<Long, Object> parcial) {
        return idsEsperados.stream().filter(id -> !parcial.containsKey(id)).collect(Collectors.toList());
    }

    public static void finalizarColeta(ExecucaoContexto ctx, String salvarEm, Map<Long, Object> parcial) {
        ctx.getContexto().put(salvarEm, parcial);
        ctx.getContexto().remove("_partial_" + salvarEm);
    }

    /** Coerção defensiva: chaves/valores vindos do contexto podem ser Long, Integer ou String
     *  dependendo de terem passado por um round-trip de (de)serialização JSON. */
    public static Long comoLong(Object v) {
        if (v instanceof Long l)   return l;
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) return Long.parseLong(s);
        return null;
    }
}
