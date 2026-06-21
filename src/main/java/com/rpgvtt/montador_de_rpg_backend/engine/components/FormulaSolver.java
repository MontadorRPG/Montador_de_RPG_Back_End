package com.rpgvtt.montador_de_rpg_backend.engine.components;

import com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador.Operations;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador.Procedures;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FormulaSolver{ 

    public static double execute(String Formula) {
        List<Object> tokens = Procedures.cloneTokens(Formula);
        // Converter para RPN
        List<Object> rpn = formulaTranslator(tokens);
        // Avaliar RPN
        double finalResult = solver(rpn);
        System.out.printf("\nResultado Final: %.4f\n", finalResult);
        return finalResult;
    }

    private static List<Object> formulaTranslator(List<Object> tokens) {
        Stack<String> operadores = new Stack<>();
        List<Object> output = new ArrayList<>();

        for (Object token : tokens) {
            if (token instanceof Double) {
                output.add(token);
            } else {
                String op = (String) token;
                if (op.equals("(")) {
                    operadores.push(op);
                } else if (op.equals(")")) {
                    while (!operadores.isEmpty() && !operadores.peek().equals("(")) {
                        output.add(operadores.pop());
                    }
                    operadores.pop(); // remove "("
                } else {
                    while (!operadores.isEmpty() && Procedures.priority(operadores.peek()) >= Procedures.priority(op)) {
                        output.add(operadores.pop());
                    }
                    operadores.push(op);
                }
            }
        }
        while (!operadores.isEmpty()) {
            output.add(operadores.pop());
        }
        return output;
    }



    private static double solver(List<Object> tokens) {
        Stack<Double> stack = new Stack<>();
        for (Object token : tokens) {
            if (token instanceof Double) {
                stack.push((Double) token);
            } else {
                String op = (String) token;
                double b = stack.pop();
                double a = stack.pop();
                stack.push(Operations.execute(op, a, b));
            }
        }
        return stack.pop();
    }
}