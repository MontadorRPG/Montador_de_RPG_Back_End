package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.rpgvtt.montador_de_rpg_backend.engine.components.InterpretadorJson;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InterpretadorContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class AcumularValorHandler implements EtapaHandler {

    private final InterpretadorJson interpretador;
    private final InstanciaResolver instanciaResolver;
    private final CenaRepository cenaRepo;
    private final JsonMapper mapper;

    @Override
    public String tipoEtapa() { return "ACUMULAR_VALOR"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();
        JsonNode origemNode = params.get("origem");
        String destino = params.get("destino").asString();

        Object valor;
        if (origemNode.isObject()) {
            // Interpreta como expressão
            InterpretadorContexto intCtx = new InterpretadorContexto(
                (ProcedimentoContexto) ctx, instanciaResolver, cenaRepo, mapper);
            valor = interpretador.interpretar(origemNode, intCtx).getValor();
        } else if (origemNode.isString()) {
            // Chave de contexto
            valor = ctx.getContexto().get(origemNode.asString(), Object.class).orElse(0);
        } else {
            throw new IllegalArgumentException("'origem' deve ser texto ou objeto (expressão)");
        }

        List<Object> lista = ctx.getContexto().getList(destino);
        lista.add(valor);
        return ResultadoEtapa.concluida(Map.of("status", "adicionado", "valor", valor, "tamanho", lista.size()));
    }
}
