package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CalculadorHandler implements EtapaHandler {

    @Override
    public String tipoEtapa() { return "CALCULAR"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();
        
        String listaOrigem = params.get("lista").asString();
        String operacao    = params.get("operacao").asString(); // "MAX", "MIN", "SUM", "AVG"
        String salvarEm    = params.get("salvar_em").asString();

        // Pega a lista (usando seu método getList que criamos)
        List<Object> lista = ctx.getContexto().getList(listaOrigem);
        
        if (lista.isEmpty()) {
            throw new IllegalArgumentException("Lista vazia para cálculo: " + listaOrigem);
        }

        // Converte para Double para garantir cálculo numérico
        List<Double> numeros = lista.stream()
                .map(obj -> Double.valueOf(obj.toString()))
                .collect(Collectors.toList());

        double resultado = switch (operacao.toUpperCase()) {
            case "MAX" -> numeros.stream().mapToDouble(v -> v).max().orElse(0);
            case "MIN" -> numeros.stream().mapToDouble(v -> v).min().orElse(0);
            case "SUM" -> numeros.stream().mapToDouble(v -> v).sum();
            case "AVG" -> numeros.stream().mapToDouble(v -> v).average().orElse(0);
            default -> throw new IllegalArgumentException("Operação não suportada: " + operacao);
        };

        // Salva o resultado
        ctx.getContexto().put(salvarEm, resultado);

        return ResultadoEtapa.concluida(Map.of(
            "operacao", operacao,
            "resultado", resultado
        ));
    }
}