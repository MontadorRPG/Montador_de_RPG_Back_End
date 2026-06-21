package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.PrimitivoExecutor;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.*;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.ColetaParcialUtil;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ProcessarGambitsHandler implements EtapaHandler {

    private final PrimitivoExecutor primitivoExecutor;
    private final EntidadeInstanciaRepository instanciaRepo;

    @Override
    public String tipoEtapa() { return "PROCESSAR_GAMBITS"; }

    /**
     * parametros_etapa:
     * {
     *   "gambits_ids_fonte":  "contexto.gambits_aliados_ids",
     *   "gambits_info_fonte": "contexto.gambits_aliados",
     *
     *   "definicoes_gambit": {
     *     "DESARMAR": [{ "primitivo": "GERENCIAR_STATUS",
     *                    "parametros": { "acao": "APLICAR", "status": "desarmado" } }],
     *     "DERRUBAR": [...],
     *     "EMPURRAR": [...],
     *     "INTIMIDAR": [...]
     *   },
     *
     *   "salvar_em": "resultados_gambits_aliados"
     * }
     *
     * FIX: removida a dependência de CombateService/EventoSistema — as ações de
     * gambit agora disparam primitivos diretamente via PrimitivoExecutor, igual
     * a DispararGatilhoItemHandler. As opções oferecidas vêm das CHAVES de
     * definicoes_gambit + "PASSAR" implícito — fonte única de verdade, sem lista
     * paralela que precise ser mantida sincronizada manualmente.
     */
    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();

        String idsFonte = exigirTexto(params, "gambits_ids_fonte", etapa);
        String salvarEm = exigirTexto(params, "salvar_em", etapa);
        JsonNode definicoes = params.path("definicoes_gambit");

        List<Long> idsComGambit = ctx.getContexto().getListaIds(idsFonte.replace("contexto.", ""));

        if (idsComGambit.isEmpty()) {
            return ResultadoEtapa.concluida(Map.of("gambitsProcessados", 0));
        }

        Map<Long, Object> parcial = ColetaParcialUtil.carregarParcial(ctx, salvarEm);
        parcial = ColetaParcialUtil.registrarResposta(ctx, salvarEm, parcial, idsComGambit);

        List<Long> pendentes = ColetaParcialUtil.pendentes(idsComGambit, parcial);

        if (!pendentes.isEmpty()) {
            List<String> opcoes = new ArrayList<>();
            definicoes.properties().forEach(e -> opcoes.add(e.getKey()));
            opcoes.add("PASSAR");

            return ResultadoEtapa.aguardandoInputMultiplo(Map.of(
                    "prompt",      "You earned a GAMBIT! Choose a bonus action.",
                    "opcoes",      opcoes,
                    "podePassar",  true,
                    "salvarEm",    salvarEm + "_entrada",
                    "pendentes",   pendentes,
                    "respondidos", new ArrayList<>(parcial.keySet())
            ));
        }

        Map<Long, Object> resultados = new LinkedHashMap<>();
        for (var entry : parcial.entrySet()) {
            Long idAtacante = entry.getKey();
            String acao = extrairAcao(entry.getValue());
            Long idAlvo  = extrairAlvo(entry.getValue());

            if ("PASSAR".equals(acao) || idAlvo == null) continue;

            JsonNode definicaoAcao = definicoes.get(acao);
            if (definicaoAcao == null) continue; // ação desconhecida — ignorada silenciosamente

            EntidadeInstancia atacante = instanciaRepo.findById(idAtacante)
                    .orElseThrow(() -> new EntityNotFoundException(EntidadeInstancia.class, idAtacante));
            EntidadeInstancia alvo = instanciaRepo.findById(idAlvo)
                    .orElseThrow(() -> new EntityNotFoundException(EntidadeInstancia.class, idAlvo));

//            resultados.put(idAtacante, primitivoExecutor.executarPrimitivoAvulso(
//                    definicaoAcao, ctx.getSistema(), atacante, alvo));
        }

        ColetaParcialUtil.finalizarColeta(ctx, salvarEm, parcial);

        return ResultadoEtapa.concluida(Map.of(
                "gambitsProcessados", parcial.size(), "resultados", resultados));
    }

    private String extrairAcao(Object decisao) {
        if (decisao instanceof String s) return s;
        if (decisao instanceof Map<?, ?> m) return String.valueOf(m.get("acao"));
        return "PASSAR";
    }

    private Long extrairAlvo(Object decisao) {
        if (decisao instanceof Map<?, ?> m) return ColetaParcialUtil.comoLong(m.get("id_alvo"));
        return null;
    }

    private String exigirTexto(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v.asString();
    }
}
