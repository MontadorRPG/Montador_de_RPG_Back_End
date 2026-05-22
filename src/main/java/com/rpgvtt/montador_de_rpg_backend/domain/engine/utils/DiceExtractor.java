package com.rpgvtt.montador_de_rpg_backend.domain.engine.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiceExtractor {

    /**
     * Varre a fórmula do sistema procurando por rolagens de dados (Ex: 1d20, 3d6, d10)
     * para que o sistema saiba o que pedir para o frontend animar.
     */
    public static List<String> extrairDados(String formula) {
        List<String> dadosRequisitados = new ArrayList<>();
        
        // Expressão regular para capturar o padrão clássico de dados de RPG
        Pattern pattern = Pattern.compile("\\b(\\d+)?d(\\d+)\\b");
        Matcher matcher = pattern.matcher(formula);
        
        while (matcher.find()) {
            dadosRequisitados.add(matcher.group());
        }
        
        return dadosRequisitados;
    }
}