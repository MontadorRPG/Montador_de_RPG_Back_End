package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.engine.components.InterpretadorJson;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InterpretadorContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador.ResultadoExpressao;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaRepository;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DefinirVariavelHandler implements EtapaHandler {

    private final InterpretadorJson interpretador;
    private final InstanciaResolver instanciaResolver;
    private final CenaRepository cenaRepo;
    private final JsonMapper mapper;

    @Override
    public String tipoEtapa() { return "DEFINIR_VARIAVEL"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();
        String nomeVariavel = exigirTexto(params, "nome", etapa);
        JsonNode expressao = params.get("expressao");

        if (expressao == null || expressao.isMissingNode()) {
            throw new JsonFieldNotFoundException("expressao", etapa.getNome());
        }

        // Cria o contexto de avaliação do Interpretador
        InterpretadorContexto intCtx = new InterpretadorContexto((ProcedimentoContexto) ctx, instanciaResolver, cenaRepo, mapper);
        
        // Avalia a expressão JSON
        ResultadoExpressao resultado = interpretador.interpretar(expressao, intCtx);

        // Salva o valor no contexto do procedimento com base no tipo retornado
        switch (resultado.getTipo()) {
            case NUMERO -> ctx.getContexto().put(nomeVariavel, resultado.comoNumero());
            case TEXTO -> ctx.getContexto().put(nomeVariavel, resultado.comoTexto());
            case BOOLEANO -> ctx.getContexto().put(nomeVariavel, resultado.comoBooleano());
            case LISTA -> ctx.getContexto().put(nomeVariavel, resultado.comoLista());
            case OBJETO -> ctx.getContexto().put(nomeVariavel, resultado.getValor());
            default -> ctx.getContexto().put(nomeVariavel, null);
        }

        return ResultadoEtapa.concluida(Map.of(
            "variavel", nomeVariavel,
            "valor_salvo", resultado.getValor()
        ));
    }

    private String exigirTexto(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v.asString();
    }
}