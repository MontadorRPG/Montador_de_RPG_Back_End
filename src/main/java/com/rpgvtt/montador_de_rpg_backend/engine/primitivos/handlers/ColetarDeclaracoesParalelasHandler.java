package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.ColetaParcialUtil;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.FonteListaResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ColetarDeclaracoesParalelasHandler implements EtapaHandler {

    private final FonteListaResolver fonteListaResolver;

    @Override
    public String tipoEtapa() { return "COLETAR_DECLARACOES_PARALELAS"; }

    /**
     * parametros_etapa:
     * {
     *   "grupo_fonte": "batalha.time.0" | "contexto.<chave>" | "estatico" | "config_sistema.<chave>",
     *   "grupo_estatico": [...],            -- quando grupo_fonte = estatico
     *   "grupo_filtro_vivo": true,          -- opcional
     *   "grupo_atributo_vida": "vigour",    -- opcional, default "vigour"
     *
     *   "prompt": "...",
     *   "opcoes_fonte": "batalha.time.1" | "contexto.<chave>" | "estatico" | "config_sistema.<chave>",
     *   "opcoes_estatico": [...],
     *   "opcoes_filtro_vivo": true,
     *   "opcoes_atributo_vida": "vigour",
     *
     *   "pode_passar": false,
     *   "salvar_em": "declaracoes_knights"
     * }
     */
    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();

        String salvarEm   = exigirTexto(params, "salvar_em", etapa);
        String prompt     = params.path("prompt").asString("Declare your action");
        boolean podePasar = params.path("pode_passar").asBoolean(true);

        List<Long> idsEsperados = fonteListaResolver.resolverIds(
                exigirTexto(params, "grupo_fonte", etapa),
                params.get("grupo_estatico"), ctx,
                params.path("grupo_filtro_vivo").asBoolean(false),
                params.path("grupo_atributo_vida").asString("vigour")
        );

        Map<Long, Object> parcial = ColetaParcialUtil.carregarParcial(ctx, salvarEm);
        parcial = ColetaParcialUtil.registrarResposta(ctx, salvarEm, parcial, idsEsperados);

        List<Long> pendentes = ColetaParcialUtil.pendentes(idsEsperados, parcial);

        if (pendentes.isEmpty()) {
            ColetaParcialUtil.finalizarColeta(ctx, salvarEm, parcial);
            return ResultadoEtapa.concluida(Map.of(
                    "totalRespondentes", parcial.size(), salvarEm, parcial));
        }

        List<String> opcoes = fonteListaResolver.resolverIdsComoTexto(
                exigirTexto(params, "opcoes_fonte", etapa),
                params.get("opcoes_estatico"), ctx,
                params.path("opcoes_filtro_vivo").asBoolean(false),
                params.path("opcoes_atributo_vida").asString("vigour")
        );

        return ResultadoEtapa.aguardandoInputMultiplo(Map.of(
                "prompt",        prompt,
                "opcoes",        opcoes,
                "podePassar",    podePasar,
                "salvarEm",      salvarEm + "_entrada",   // FIX: camelCase — frontend reads inputSolicitado.salvarEm
                "respondidos",   new ArrayList<>(parcial.keySet()),
                "pendentes",     pendentes,
                "totalEsperado", idsEsperados.size()
        ));
    }

    private String exigirTexto(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v.asString();
    }
}
