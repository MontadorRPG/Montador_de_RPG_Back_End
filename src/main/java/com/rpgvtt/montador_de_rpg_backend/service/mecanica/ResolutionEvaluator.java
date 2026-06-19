package com.rpgvtt.montador_de_rpg_backend.service.mecanica;

import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.Resolucao;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResolutionEvaluator {

    private final RandomProvider randomProvider;

    public ResolutionOutcome evaluate(Resolucao resolucao, JsonNode contexto) {
        String tipo = resolucao.getTipo();

        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de resolução não informado.");
        }

        if (tipo.equalsIgnoreCase("d20_leq_attribute") || tipo.equalsIgnoreCase("d20_leq") || tipo.equalsIgnoreCase("d20_leq_value")) {
            JsonNode params = resolucao.getParametros();

            // Priority: valor (literal) -> valores (array with modo) -> atributo/caminho in contexto
            Integer target = null;
            String sourceDesc = "";

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

            int roll = randomProvider.rollD20();
            boolean success = roll <= target;

            JsonNode detalhes = params;
            String motivo = String.format("d20 <= alvo (%s): %d <= %d", sourceDesc, roll, target);

            return new ResolutionOutcome(roll, target, success, motivo, detalhes);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de resolução não suportado: " + tipo);
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
        if (node.has("atual")) { // support structures like { "atual": 5 }
            JsonNode atual = node.get("atual");
            if (atual.isNumber()) return atual.asInt();
        }
        return null;
    }
}
