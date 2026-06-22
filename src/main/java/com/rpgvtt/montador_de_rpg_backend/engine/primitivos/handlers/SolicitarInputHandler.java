package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SolicitarInputHandler implements EtapaHandler {

    private final InstanciaResolver instanciaResolver;

    @Override
    public String tipoEtapa() { return "SOLICITAR_INPUT"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();

        String ctxKey       = exigirCampo(params, "salvar_em", etapa);
        String campoPedido  = exigirCampo(params, "campo_pedido", etapa);
        boolean podePassar  = params.path("pode_passar").asBoolean(true);
        String recursoNecessario = params.path("recurso_necessario").asString(null);

        if (recursoNecessario != null && !ctx.semInstancias()) {
            EntidadeInstancia inst = instanciaResolver.retornarAtiva(ctx);
            JsonNode val = inst.getAtributosAtuais().path(recursoNecessario);
            boolean disponivel = val.isBoolean() ? val.asBoolean()
                    : val.isNumber() && val.asLong() > 0;
            if (!disponivel) {
                return ResultadoEtapa.concluida(Map.of(
                        "campoPedido", campoPedido, "status", "recurso_indisponivel"));
            }
        }

        if (!ctx.getContexto().containsKey(ctxKey)) {
            return ResultadoEtapa.aguardandoInput(Map.of(
                    "campoPedido",  campoPedido,
                    "opcoes",       resolverOpcoes(params, ctx),
                    "pode_passar",  podePassar,
                    "salvar_em",    ctxKey
            ));
        }

        Object raw = ctx.getContexto().get(ctxKey, Object.class).orElse(null);
        String escolha = raw != null ? raw.toString() : null;

        if ("PASSAR".equals(escolha) && podePassar) {
            return ResultadoEtapa.concluida(Map.of("campoPedido", campoPedido, "status", "passou"));
        }

        return ResultadoEtapa.concluida(Map.of("campoPedido", campoPedido, "escolha", escolha));
    }

    private String exigirCampo(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) {
            throw new JsonFieldNotFoundException(chave, etapa.getNome());
        }
        return v.asString();
    }

    private List<String> resolverOpcoes(JsonNode params, ExecucaoContexto ctx) {
        String fonte = params.path("opcoes_fonte").asString("");

        if ("estatico".equals(fonte)) {
            return jsonArrayParaLista(params.path("opcoes_estatico"));
        }

        if (fonte.startsWith("config_sistema.")) {
            String chave = fonte.substring("config_sistema.".length());
            JsonNode raw = ctx.getSistema().getConfiguracao().path(chave);
            if (raw.isMissingNode()) {
                log.warn("config_sistema.{} não encontrado no sistema {}", chave, ctx.getIdSistema());
                return List.of();
            }
            return jsonArrayParaLista(raw);
        }

        if (fonte.startsWith("contexto.")) {
            String chave = fonte.substring("contexto.".length());
            return ctx.getContexto().get(chave, List.class).orElseGet(() -> {
                log.warn("Chave '{}' não encontrada no contexto do procedimento", chave);
                return List.of();
            });
        }

        if (fonte.startsWith("atributos_instancia.")) {
            String chave = fonte.substring("atributos_instancia.".length());
            if (ctx.semInstancias()) {
                log.warn("opcoes_fonte '{}' requer instância mas escopo é NENHUMA", fonte);
                return List.of();
            }
            EntidadeInstancia inst = instanciaResolver.retornarAtiva(ctx);
            return jsonArrayParaLista(inst.getAtributosAtuais().path(chave));
        }

        log.warn("opcoes_fonte desconhecido: '{}'", fonte);
        return List.of();
    }

    private List<String> jsonArrayParaLista(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        node.forEach(n -> {
            if (n.isObject()) {
                // Tenta extrair o campo "valor"; se não existir, usa "label"
                String valor = n.has("valor") ? n.get("valor").asString() : n.path("label").asString(null);
                out.add(valor != null ? valor : n.toString());
            } else {
                out.add(n.asString());
            }
        });
        return out;
    }
}
