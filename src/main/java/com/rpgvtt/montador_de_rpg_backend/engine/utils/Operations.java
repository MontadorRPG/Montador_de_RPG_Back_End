package com.rpgvtt.montador_de_rpg_backend.engine.utils;

 public class Operations{
	
    public static double execute(String operator, double valueA, double valueB) {
       
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
                if (valueB < 0) yield 0.0;
                if (valueA == 2.0) yield Math.sqrt(valueB);
                else if (valueA == 3.0) yield Math.cbrt(valueB);
                else yield Math.pow(valueB, 1.0 / valueA);
            }
            default -> throw new IllegalArgumentException("Operador matemático desconhecido: " + operator);
        };
   }

}