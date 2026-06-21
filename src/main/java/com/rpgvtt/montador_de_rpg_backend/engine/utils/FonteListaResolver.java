package com.rpgvtt.montador_de_rpg_backend.engine.utils;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolve uma lista de IDs a partir de uma fonte padronizada — usado tanto
 * para "quem deve responder" (grupo_fonte) quanto "quais opções existem"
 * (opcoes_fonte, quando as opções são IDs de instância, como escolha de alvo).
 *
 * Fontes suportadas:
 *   "estatico"               -> lista literal de IDs embutida no params
 *   "config_sistema.<chave>" -> lista declarada em Sistema.configuracao
 *   "contexto.<chave>"       -> lista de IDs salva previamente no contexto
 *   qualquer outra string    -> delegado ao InstanciaResolver (batalha.*,
 *                               instancia_ativa, todos_participantes, etc.)
 *
 * filtro_vivo + atributo_vida aplicam o mesmo critério de "ainda em jogo"
 * usado por LISTAR_INSTANCIAS, sobre QUALQUER fonte acima — inclusive sobre
 * listas já salvas em contexto, caso não tenham sido pré-filtradas.
 *
 * Nota de escopo: este resolver assume que as opções são IDs (Long). Para
 * opções de texto livre (nomes de ação como "ATACAR"), use SOLICITAR_INPUT,
 * que já trata listas estáticas de strings diretamente.
 */
@Component
@RequiredArgsConstructor
public class FonteListaResolver {

    private final InstanciaResolver instanciaResolver;

    public List<Long> resolverIds(String fonte, JsonNode literalNode, ExecucaoContexto ctx,
                                  boolean filtroVivo, String atributoVida) {
        List<Long> ids = resolverIdsBruto(fonte, literalNode, ctx);
        if (!filtroVivo) return ids;

        return ids.stream()
                .map(instanciaResolver::buscarPorId)
                .filter(i -> i.getAtributosAtuais().path(atributoVida).asInt(0) > 0)
                .map(EntidadeInstancia::getId)
                .toList();
    }

    public List<String> resolverIdsComoTexto(String fonte, JsonNode literalNode, ExecucaoContexto ctx,
                                             boolean filtroVivo, String atributoVida) {
        return resolverIds(fonte, literalNode, ctx, filtroVivo, atributoVida)
                .stream().map(String::valueOf).toList();
    }

    private List<Long> resolverIdsBruto(String fonte, JsonNode literalNode, ExecucaoContexto ctx) {
        if ("estatico".equals(fonte))            return paraListaLong(literalNode);
        if (fonte.startsWith("contexto."))       return ctx.getContexto()
                .getListaIds(fonte.substring("contexto.".length()));
        if (fonte.startsWith("config_sistema.")) return paraListaLong(
                ctx.getSistema().getConfiguracao().path(fonte.substring("config_sistema.".length())));

        // batalha.*, instancia_ativa, todos_participantes, atributos_instancia.*
        return instanciaResolver.resolverDeFonte(fonte, ctx).stream()
                .map(EntidadeInstancia::getId)
                .toList();
    }

    private List<Long> paraListaLong(JsonNode arr) {
        List<Long> out = new ArrayList<>();
        if (arr != null && arr.isArray()) arr.forEach(n -> out.add(n.asLong()));
        return out;
    }
}
