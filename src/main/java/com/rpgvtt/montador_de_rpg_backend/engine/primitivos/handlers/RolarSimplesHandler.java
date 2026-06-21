package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.engine.components.ParametroResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine.ResultadoRolagem;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine.RolagemEngine;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine.VantagemTipo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RolarSimplesHandler implements EtapaHandler {

    private final RolagemEngine rolagemEngine;
    private final ParametroResolver resolver;

    @Override
    public String tipoEtapa() { return "ROLAR_SIMPLES"; }

    /**
     * parametros_etapa:
     * {
     *   "dado": "d20", "quantidade": 1, "explosao": false,
     *   "vantagem": "NORMAL" | "VANTAGEM" | "DESVANTAGEM",
     *
     *   "modificador": {"fonte": "atributo", "chave": "forca"}, // opcional
     *
     *   "modo_sucesso": "AUSENTE" | "ROLAR_IGUAL_OU_MAIOR" | "ROLAR_IGUAL_OU_MENOR",
     *   "valor_alvo": {"fonte": "fixo", "chave": 15 }
     *               | {"fonte": "atributo", "chave": "VIG"}, // Save MB: rolar <= VIG
     *
     *   "salvar_em": "resultado_roll"
     * }
     */
    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();

        String dado = exigirTexto(params, "dado", etapa);
        int quantidade = params.path("quantidade").asInt(1);
        boolean explosao = params.path("explosao").asBoolean(false);
        VantagemTipo vantagem = VantagemTipo.valueOf(params.path("vantagem").asString("NORMAL"));
        String salvarEm = params.path("salvar_em").asString(null);

        ResultadoRolagem rolo = rolagemEngine.executarComVantagem(
                RolagemEngine.simples(dado, quantidade, explosao), vantagem);

        double modificador = resolver.resolverNumero(params.get("modificador"), ctx);
        double total = rolo.total() + modificador;

        int faces = rolagemEngine.parseFaces(dado);
        boolean critico       = quantidade == 1 && rolo.isCritico(faces);
        boolean falhaCritica  = quantidade == 1 && rolo.isFalhaCritica();

        Boolean sucesso = avaliarSucesso(params, ctx, total, etapa);

        if (salvarEm != null) {
            ctx.getContexto().put(salvarEm, (long) total);
            ctx.getContexto().put(salvarEm + "_rolos", rolo.rolos());
            if (sucesso != null) ctx.getContexto().put(salvarEm + "_sucesso", sucesso);
        }

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("dado", dado);
        dados.put("rolos", rolo.rolos());
        dados.put("modificador", modificador);
        dados.put("total", total);
        dados.put("critico", critico);
        dados.put("falhaCritica", falhaCritica);
        if (sucesso != null) dados.put("sucesso", sucesso);

        return ResultadoEtapa.concluida(dados);
    }

    private Boolean avaliarSucesso(JsonNode params, ExecucaoContexto ctx, double total, EtapaExecutavel etapa) {
        String modo = params.path("modo_sucesso").asString("AUSENTE");
        if ("AUSENTE".equals(modo)) return null;

        JsonNode valorAlvo = params.get("valor_alvo");
        if (valorAlvo == null) throw new JsonFieldNotFoundException("valor_alvo", etapa.getNome());
        double alvo = resolver.resolverNumero(valorAlvo, ctx);

        return switch (modo) {
            case "ROLAR_IGUAL_OU_MAIOR" -> total >= alvo;   // d20 vs CA/CD — estilo D&D
            case "ROLAR_IGUAL_OU_MENOR" -> total <= alvo;   // Save de Mythic Bastionland
            default -> throw new IllegalArgumentException("modo_sucesso desconhecido: " + modo);
        };
    }

    private String exigirTexto(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v.asString();
    }
}
