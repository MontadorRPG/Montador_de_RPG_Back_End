package com.rpgvtt.montador_de_rpg_backend.engine;

import com.rpgvtt.montador_de_rpg_backend.engine.utils.Operations;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.Procedures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class FormulaSolver{ 

    public void solver(String Formula, ArrayList MathBlocks){
        System.out.println("Expressão original: '" + Formula + "'");
        List<String> tokens = Procedures.cloneTokens(Formula);
        System.out.println("Split: " + tokens);
        String result = formulaTranslator(tokens);
        int finalResult = Integer.parseInt(result);
        System.out.println("\nResultado Final: '" + finalResult + "'\nEnd");
    }

    private static String formulaTranslator(List<String> tokens) {
        // 1º - (Tudo Dentro) -> Resolve os parênteses primeiro
        while (tokens.contains("(")) {
            int openingIndex = tokens.lastIndexOf("("); // Pega o último parênteses aberto (mais interno)
            int closingIndex = tokens.subList(openingIndex, tokens.size()).indexOf(")") + openingIndex; // Acha o fechamento correspondente
            
            if (closingIndex == -1) {
                throw new IllegalArgumentException("Expressão mal-formada: Parênteses não fechado.");
            }

            System.out.println("\n--- Resolvendo Parênteses Interno ---");

            // Isola o que está dentro: sublista entre a abertura e o fechamento
            List<String> subExpression = new ArrayList<>(tokens.subList(openingIndex + 1, closingIndex));
            System.out.println("Sub-Expressão encontrada: " + subExpression);
            
            // Avalia recursivamente o conteúdo de dentro do parênteses
            String resultSub = formulaTranslator(subExpression);
            System.out.println("Resultado do Parênteses: " + resultSub);

            // Substitui todo o bloco de parênteses pelo resultado obtido
            // Remove do fim para o começo para não perder os índices corretos
            for (int i = closingIndex; i >= openingIndex; i--) {
                tokens.remove(i);
            }
            tokens.add(openingIndex, resultSub);
            System.out.println("Estado da expressão principal: " + tokens);
            System.out.println("-------------------------------------");
        }
        // 2º Passagem - Processa '^' ou '\|'
        Operations.process(tokens, Arrays.asList("^", "\\|"));

        // 3º Passagem - Processa '*' ou '/'
        Operations.process(tokens, Arrays.asList("*", "/"));

        // 4º Passagem - Processa '+' ou '-'
        Operations.process(tokens, Arrays.asList("+", "-"));
        return tokens.get(0);
    }
}