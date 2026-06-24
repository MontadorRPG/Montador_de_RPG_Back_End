package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.components.InterpretadorJson;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InterpretadorContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador.contexto.Contexto;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaRepository;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador.ResultadoExpressao;
// import com.rpgvtt.montador_de_rpg_backend.repository.batalha.cenaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class VerificarRepeticaoHandler implements EtapaHandler {

    private final InterpretadorJson interpretador;
    private final InstanciaResolver instanciaResolver;
    private final CenaRepository cenaRepo;
    private final JsonMapper mapper;

    @Override
    public String tipoEtapa() {
        return "VERIFICAR_REPETICAO";
    }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        return null;
    }

    public ResultadoEtapa executar(EtapaProcedimento etapa, ProcedimentoContexto ctx) {
        // parametros_etapa is already JsonNode — no objectMapper needed
        JsonNode params = etapa.getParametrosEtapa(); // returns raw JsonNode

        int maxIteracoes = params.path("max_iteracoes").asInt(500);
        if (ctx.getEtapaAtual() >= maxIteracoes) {
            return ResultadoEtapa.erro(
                    "Limite de iterações atingido (" + maxIteracoes + ") em '" +
                            ctx.getProcedimento().getNome() + "'");
        }

        JsonNode expressaoCondicao = params.get("condicao");
        if (expressaoCondicao == null) {
            return ResultadoEtapa.erro("REPETIR_SE sem campo 'condicao'");
        }

        Contexto contexto = new InterpretadorContexto(ctx, instanciaResolver, cenaRepo, mapper);
        ResultadoExpressao resultado = interpretador.interpretar(expressaoCondicao, contexto);

        if (resultado.getTipo() != ResultadoExpressao.TipoResultado.BOOLEANO) {
            return ResultadoEtapa.erro(
                    "REPETIR_SE: condicao deve retornar booleano, retornou "
                            + resultado.getTipo());
        }

        if (!resultado.comoBooleano()) {
            return ResultadoEtapa.concluida(Map.of(
                    "loop_encerrado",  true,
                    "total_iteracoes", ctx.getEtapaAtual()
            ));
        }

        // Clean transient keys
        JsonNode limparNode = params.get("limpar_contexto");
        if (limparNode != null && limparNode.isArray()) {
            limparNode.forEach(n -> ctx.getContexto().remove(n.asString()));
        }

        int ordemAlvo  = params.get("voltar_para_ordem").asInt();
        int indiceAlvo = ctx.indiceParaOrdem(ordemAlvo);
        ctx.setEtapaAtual(indiceAlvo);
        ctx.incrementarIteracao();

        return ResultadoEtapa.repetindo(Map.of(
                "iteracao",            ctx.getIteracaoAtual(),
                "voltando_para_ordem", ordemAlvo
        ));
    }
}
