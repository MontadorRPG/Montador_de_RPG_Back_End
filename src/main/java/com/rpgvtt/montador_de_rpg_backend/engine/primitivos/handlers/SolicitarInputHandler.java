package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.*;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class SolicitarInputHandler implements EtapaHandler {

    private final InstanciaResolver instanciaResolver;
    private JsonMapper mapper;

    @Override
    public String tipoEtapa() {
        return "SOLICITAR_INPUT";
    }

    @Override
    public ResultadoEtapa executar(EtapaProcedimento etapa, ProcedimentoContexto ctx) {
        Map<String, Object> params = mapper.convertValue(etapa.getParametrosEtapa(), new TypeReference<>() {});
        String ctxKey = params.get("salvar_em").toString();
        String campoPedido =  params.get("campo_pedido").toString();
        boolean podePassar = (boolean) params.getOrDefault("pode_passar", true);

        String recursoNecessario = params.get("recurso_necessario").toString();
        if (recursoNecessario != null && !ctx.semInstancias()) {

            EntidadeInstancia inst = instanciaResolver.retornarAtiva(ctx);
            Map<String, Object> instAtributos = mapper
                    .convertValue(inst.getAtributosAtuais(), new TypeReference<>() {});
            Object val = instAtributos.get(recursoNecessario);
            boolean disponivel = (val instanceof Boolean b && b) || (val instanceof Number n && n.longValue() > 0);
            if (!disponivel) {
                return ResultadoEtapa.concluida(campoPedido + "nao disponível");
            }
        }

        if (!ctx.getContexto().containsKey(ctxKey)) {
            return ResultadoEtapa.aguardandoInput(Map.of(
                    "campoPedido", campoPedido,
                    "opcoes", resolverOpcoes(params, ctx),
                    "pode_passar", podePassar,
                    "salvar_em", ctxKey
            ));
        }

        String escolha = ctx.getContexto().getStringOrThrow(ctxKey);

        // Muito simples por enquanto, estrutura básica
        if ("PASSAR".equals(escolha) && podePassar) {
            return ResultadoEtapa.concluida(campoPedido + "passar");
        }

        return ResultadoEtapa.concluida(Map.of("Campo pedido", campoPedido, "Escolha", escolha));
    }

    @SuppressWarnings("unchecked")
    private List<String> resolverOpcoes(Map<String, Object> params, ProcedimentoContexto ctx) {

        String fonte = params.get("opcoes_fonte").toString();

        if ("estatico".equals(fonte)) {
            Object raw = params.get("opcoes_estatico");
            if (raw == null) return List.of();
            return (List<String>) raw;
        }

        if (fonte.startsWith("config_sistema.")) {
            String chave = fonte.substring("config_sistema.".length());
            Object raw = ctx.getSistema().getConfiguracao().get(chave);
            if (raw == null) {
                log.warn("config_sistema.{} não encontrado no sistema {}",
                        chave, ctx.getIdSistema());
                return List.of();
            }
            return (List<String>) raw;
        }

        if (fonte.startsWith("contexto.")) {
            String chave = fonte.substring("contexto.".length());
            Object raw = ctx.getContexto().get(chave, List.class);
            if (raw == null) {
                log.warn("Chave '{}' não encontrada no contexto do procedimento", chave);
                return List.of();
            }
            return (List<String>) raw;
        }

        if (fonte.startsWith("atributos_instancia.")) {
            String chave = fonte.substring("atributos_instancia.".length());

            if (ctx.semInstancias()) {
                log.warn("opcoes_fonte '{}' requer instância mas escopo é NENHUMA", fonte);
                return List.of();
            }

            // Resolver esse caso dps: if (ctx.getEscopo() instanceof EscopoInstancias.Multiplas)

            EntidadeInstancia inst = instanciaResolver.retornarAtiva(ctx);
            Object raw = inst.getAtributosAtuais().get(chave);
            if (raw == null) return List.of();
            return (List<String>) raw;
        }

        log.warn("opcoes_fonte desconhecido: '{}'", fonte);
        return List.of();
    }

}
