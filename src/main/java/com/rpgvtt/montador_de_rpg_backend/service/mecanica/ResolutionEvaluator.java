package com.rpgvtt.montador_de_rpg_backend.service.mecanica;

import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.Resolucao;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResolutionEvaluator {

    private final RandomProvider randomProvider; // mantido apenas para fallback do d20_leq

    public ResolutionOutcome evaluate(Resolucao resolucao, JsonNode contexto) {
        String tipo = resolucao.getTipo();

        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de resolução não informado.");
        }

        // Tipo legado
        if (tipo.equalsIgnoreCase("d20_leq_attribute") || tipo.equalsIgnoreCase("d20_leq") || tipo.equalsIgnoreCase("d20_leq_value")) {
            return evaluateD20Leq(resolucao, contexto);
        }

        // Novos tipos orientados a dados
        return evaluateGeneric(resolucao, contexto);
    }

    // Mantido igual ao original
    private ResolutionOutcome evaluateD20Leq(Resolucao resolucao, JsonNode contexto) {
        JsonNode params = resolucao.getParametros();

        Integer target = null;
        String sourceDesc = "";

        int roll;
        JsonNode rollNode = contexto.path("roll");
        if (rollNode.isInt()) {
            roll = rollNode.asInt();
        } else {
            roll = randomProvider.rollD20();
        }

        if (params != null && params.has("valor") && !params.get("valor").isNull()) {
            JsonNode v = params.get("valor");
            if (v.isNumber()) target = v.asInt();
            else if (v.isString()) {
                try { target = Integer.parseInt(v.asString()); } catch (NumberFormatException ignored) {}
            }
            sourceDesc = "literal:valor";
        }

        if (target == null && params != null && params.has("valores") && params.get("valores").isArray()) {
            JsonNode arr = params.get("valores");
            int best = Integer.MIN_VALUE;
            int worst = Integer.MAX_VALUE;
            for (JsonNode el : arr) {
                Integer n = null;
                if (el.isNumber()) n = el.asInt();
                else if (el.isString()) {
                    try { n = Integer.parseInt(el.asString()); } catch (NumberFormatException ignored) {}
                }
                if (n != null) {
                    best = Math.max(best, n);
                    worst = Math.min(worst, n);
                }
            }
            String modo = params.has("modo") ? params.get("modo").asString("maior") : "maior";
            if (best != Integer.MIN_VALUE) {
                target = modo.equalsIgnoreCase("menor") ? worst : best;
                sourceDesc = "array:valores[modo=" + modo + "]";
            }
        }

        if (target == null) {
            String atributoPath = null;
            if (params != null) {
                if (params.has("atributo")) atributoPath = params.get("atributo").asString(null);
                else if (params.has("caminho")) atributoPath = params.get("caminho").asString(null);
                else if (params.has("atributoCaminho")) atributoPath = params.get("atributoCaminho").asString(null);
            }

            if (atributoPath == null || atributoPath.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parâmetro 'atributo' ou 'valor(s)' obrigatório para resolução d20_leq.");
            }

            target = extractIntFromContext(contexto, atributoPath);
            if (target == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Atributo alvo não encontrado ou não numérico: " + atributoPath);
            }
            sourceDesc = "contexto:atributo(" + atributoPath + ")";
        }

        boolean success = roll <= target;
        String motivo = String.format("d20 <= alvo (%s): %d <= %d", sourceDesc, roll, target);
        return new ResolutionOutcome(roll, target, success, motivo, params);
    }

    private ResolutionOutcome evaluateGeneric(Resolucao resolucao, JsonNode contexto) {
        JsonNode params = resolucao.getParametros();
        String chaveRolagem = params.path("chave_rolagem").asString("resultado_rolagem");
        JsonNode rolagemNode = contexto.path(chaveRolagem);

        if (rolagemNode.isMissingNode()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resultado da rolagem não fornecido em '" + chaveRolagem + "'");
        }

        String tipo = resolucao.getTipo();
        Object rollValue = null;
        boolean success = false;
        String motivo = "";

        switch (tipo.toUpperCase()) {
            case "DADO_UNICO" -> {
                int roll = rolagemNode.asInt();
                rollValue = roll;
                if (params.has("dificuldade")) {
                    int dif = params.get("dificuldade").asInt();
                    success = roll >= dif;
                    motivo = String.format("dado único: %d >= %d (%s)", roll, dif, success ? "sucesso" : "falha");
                } else {
                    success = true;
                    motivo = "dado único: " + roll;
                }
            }
            case "TABELA" -> {
                int roll = rolagemNode.asInt();
                rollValue = roll;
                JsonNode entrada = findTabelaEntrada(params.get("tabela"), roll);
                success = entrada != null;
                motivo = "Tabela: roll " + roll + " -> " + (entrada != null ? entrada.toString() : "sem entrada");
            }
            case "TABELA_DUPLA" -> {
                if (!rolagemNode.isArray() || rolagemNode.size() < 2) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TABELA_DUPLA requer array com dois valores");
                }
                int roll1 = rolagemNode.get(0).asInt();
                int roll2 = rolagemNode.get(1).asInt();
                rollValue = List.of(roll1, roll2);
                JsonNode entrada = findTabelaDuplaEntrada(params.get("tabela"), roll1, roll2);
                success = entrada != null;
                motivo = String.format("Tabela Dupla: (%d,%d) -> %s", roll1, roll2, entrada != null ? entrada.toString() : "sem entrada");
            }
            case "POOL_SUCESSO" -> {
                int sucessos = rolagemNode.asInt();
                rollValue = sucessos;
                int dificuldade = params.path("dificuldade").asInt(1);
                success = sucessos >= dificuldade;
                motivo = String.format("Pool: %d sucessos, dificuldade %d -> %s", sucessos, dificuldade, success ? "sucesso" : "falha");
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de resolução não suportado: " + tipo);
        }

        return new ResolutionOutcome(rollValue, null, success, motivo, params);
    }

    // Métodos auxiliares de busca em tabelas
    private JsonNode findTabelaEntrada(JsonNode tabela, int valor) {
        if (tabela == null || !tabela.isArray()) return null;
        for (JsonNode entry : tabela) {
            if (entry.has("min") && entry.has("max")) {
                if (valor >= entry.get("min").asInt() && valor <= entry.get("max").asInt()) return entry;
            } else if (entry.has("valor") && entry.get("valor").asInt() == valor) {
                return entry;
            }
        }
        return null;
    }

    private JsonNode findTabelaDuplaEntrada(JsonNode tabela, int roll1, int roll2) {
        if (tabela == null || !tabela.isArray()) return null;
        for (JsonNode entry : tabela) {
            if (entry.has("d6") && entry.has("d12")) {
                if (entry.get("d6").asInt() == roll1 && entry.get("d12").asInt() == roll2) return entry;
            } else if (entry.has("valor1") && entry.has("valor2")) {
                if (entry.get("valor1").asInt() == roll1 && entry.get("valor2").asInt() == roll2) return entry;
            }
        }
        return null;
    }

    private Integer extractIntFromContext(JsonNode contexto, String path) {
        if (contexto == null) return null;
        String[] parts = path.split("\\.");
        JsonNode node = contexto;
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (node.has(p)) node = node.get(p);
            else return null;
        }
        if (node.isInt() || node.isLong()) return node.asInt();
        if (node.isString()) {
            try { return Integer.parseInt(node.asString()); } catch (NumberFormatException ex) { return null; }
        }
        if (node.isNumber()) return node.asInt();
        if (node.has("atual")) {
            JsonNode atual = node.get("atual");
            if (atual.isNumber()) return atual.asInt();
        }
        return null;
    }
}