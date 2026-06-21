package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.dto.mecanica.ResultadoPoolDTO;
import com.rpgvtt.montador_de_rpg_backend.engine.components.ParametroResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.*;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine.RolagemEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RolarPoolHandler implements EtapaHandler {

    private final RolagemEngine rolagemEngine;
    private final ParametroResolver resolver;

    @Override
    public String tipoEtapa() { return "ROLAR_POOL"; }

    /**
     * parametros_etapa:
     * {
     *   "tamanho_fonte": "atributos_somados" | "fixo" | "contexto",
     *   "atributos": ["Forca", "Briga"],     -- quando atributos_somados
     *   "valor": 5,                          -- quando fixo
     *   "chave_contexto": "pool_tamanho",     -- quando contexto
     *
     *   "dado": "d10", "dificuldade": 6, "conta_uns": true,
     *   "critico_em": "dois_maximos_consecutivos" | "qualquer_maximo" | null,
     *   "salvar_em": "resultado_ataque"
     * }
     */
    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();

        String dado       = exigirTexto(params, "dado", etapa);
        boolean contaUns  = params.path("conta_uns").asBoolean(false);
        String criticoEm  = params.path("critico_em").isMissingNode() || params.path("critico_em").isNull()
                ? null : params.path("critico_em").asString();
        Integer dificuldade = params.path("dificuldade").isMissingNode() || params.path("dificuldade").isNull()
                ? null : params.path("dificuldade").asInt();
        String salvarEm   = params.path("salvar_em").asString(null);

        int tamanhoPool = resolverTamanho(params, ctx, etapa);

        if (tamanhoPool <= 0) {
            if (salvarEm != null) ctx.getContexto().put(salvarEm, 0);
            return ResultadoEtapa.concluida(Map.of(
                    "tamanhoPool", 0, "rolos", List.of(), "sucessos", 0, "motivo", "pool vazio"));
        }

        ResultadoPoolDTO r = rolagemEngine.executarPool(tamanhoPool, dado, dificuldade, contaUns, criticoEm);

        if (salvarEm != null) {
            ctx.getContexto().put(salvarEm, r.sucessos());
            ctx.getContexto().put(salvarEm + "_rolos", r.rolos());
            ctx.getContexto().put(salvarEm + "_critico", r.critico());
            ctx.getContexto().put(salvarEm + "_botch", r.botch());
        }

        return ResultadoEtapa.concluida(Map.of(
                "tamanhoPool", tamanhoPool, "dado", dado,
                "dificuldade", dificuldade != null ? dificuldade : "nenhuma",
                "rolos", r.rolos(), "sucessos", r.sucessos(), "falhas", r.falhas(),
                "critico", r.critico(), "botch", r.botch()
        ));
    }

    private int resolverTamanho(JsonNode params, ExecucaoContexto ctx, EtapaExecutavel etapa) {
        String fonte = params.path("tamanho_fonte").asString("atributos_somados");
        return switch (fonte) {
            case "atributos_somados" -> resolver.resolverSomaAtributos(params.get("atributos"), ctx);
            case "fixo"     -> params.path("valor").asInt(0);
            case "contexto" -> ctx.getContexto()
                    .getInt(exigirTexto(params, "chave_contexto", etapa)).orElse(0);
            default -> throw new IllegalArgumentException("tamanho_fonte desconhecida: " + fonte);
        };
    }

    private String exigirTexto(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v.asString();
    }
}
