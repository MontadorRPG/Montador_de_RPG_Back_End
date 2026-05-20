package com.rpgvtt.montador_de_rpg_backend.engine.utils;

import java.util.List;

class Operations{

    public static void process(List<String> tokens, List<String> targetOperators) {
        for (int i = 0; i < tokens.size(); i++) {
            String tokenAtual = tokens.get(i);

            if (targetOperators.contains(tokenAtual)) {
                String operator = tokenAtual;
                double valueA = Double.parseDouble(tokens.get(i - 1));
                double valueB = Double.parseDouble(tokens.get(i + 1));

                System.out.println("Pop '" + (int)valueA + "' , '" + operator + "' , '" + (int)valueB + "'");
                System.out.println("  Operador: " + operator);
                System.out.println("  ValorA: " + (int)valueA);
                System.out.println("  ValorB: " + (int)valueB);

                double result = execute(operator, valueA, valueB);
                System.out.println("  Operations(" + operator + ") = " + (int)result);

                tokens.set(i - 1, String.valueOf((int) result));
                tokens.remove(i);     // Remove o operator
                tokens.remove(i);     // Remove o valueB

                System.out.println("  Estado atual: " + tokens);
                i--; 
            }
        }
    }
    private static double execute(String operator, double valueA, double valueB) {
        
            return switch (operator) {
                case "+" -> valueA + valueB; 
                case "-" -> valueA - valueB; 
                case "*" -> valueA * valueB; 
                case "/" -> {
                    if (valueB == 0) {
                        yield 0.0;
                    }
                    yield valueA / valueB;
                }
                case "^" -> Math.pow(valueA, valueB);
                case "\\|" -> {
                    if (valueB < 0) {
                        yield 0.0; 
                    }
                    yield Math.pow(valueB, (1.0/valueA));
                }
                default -> throw new IllegalArgumentException("Operador matemático desconhecido: " + operator);
            };
        }
}