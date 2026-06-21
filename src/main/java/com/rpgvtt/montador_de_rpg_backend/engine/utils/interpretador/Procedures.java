package com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador;

import java.util.ArrayList;
import java.util.List;

public class Procedures{
	
    public static int priority(String op) {
        return switch (op) {
            case "^", "\\|" -> 3;
            case "*", "/" -> 2;
            case "+", "-" -> 1;
            default -> 0;
        };
    }
	
    public static List<Object> cloneTokens(String expressao) {
        List<Object> list = new ArrayList<>();
        StringBuilder numero = new StringBuilder();

        for (int i = 0; i < expressao.length(); i++) {
            char c = expressao.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                numero.append(c);
            } else {
                if (numero.length() > 0) {
                    list.add(Double.parseDouble(numero.toString()));
                    numero.setLength(0);
                }
                if (!Character.isWhitespace(c)) {
                    // trata operador de raiz como dois caracteres: '\' e '|'
                    if (c == '\\' && i + 1 < expressao.length() && expressao.charAt(i + 1) == '|') {
                        list.add("\\|");
                        i++; // pula o próximo
                    } else {
                        list.add(String.valueOf(c));
                    }
                }
            }
        }
        if (numero.length() > 0) {
            list.add(Double.parseDouble(numero.toString()));
        }
        return list;
    }
    
}