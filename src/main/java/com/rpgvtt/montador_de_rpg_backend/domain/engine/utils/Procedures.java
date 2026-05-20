package com.rpgvtt.montador_de_rpg_backend.engine.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Procedures{
    // Método auxiliar para quebrar a string de forma cirúrgica (Regex)
    public static List<String> cloneTokens(String expressao) {
        List<String> list = new ArrayList<>();
        // Regex que captura: números (decimais/inteiros), o operador de raiz "\\|" ou caracteres individuais (+, -, *, /, ^, (, ))
        Pattern pattern = Pattern.compile("\\d+\\.\\d+|\\d+|\\\\\\||[+\\-*/^()]");
        Matcher matcher = pattern.matcher(expressao);
        while (matcher.find()) {
            list.add(matcher.group());
        }
        return list;
    }
}