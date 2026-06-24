package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Component
public class DefinirAlvoHandler implements EtapaHandler {

    @Override
    public String tipoEtapa() { return "DEFINIR_ALVO"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();
        String salvarEm = exigirTexto(params, "salvar_em", etapa);
        String prompt = params.path("prompt").asString("Selecione um alvo no mapa");

        // Se o contexto já tem a variável, significa que o front-end já respondeu
        if (ctx.getContexto().containsKey(salvarEm)) {
            Long alvoId = ctx.getContexto().getLongOrThrow(salvarEm);
            return ResultadoEtapa.concluida(Map.of("alvo_definido", alvoId));
        }

        // Pausa a execução e envia o payload pedindo interação no mapa
        return ResultadoEtapa.aguardandoInput(Map.of(
            "tipo_input", "SELECAO_TOKEN",
            "campoPedido", prompt,
            "salvar_em", salvarEm
        ));
    }

    private String exigirTexto(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v.asString();
    }
}