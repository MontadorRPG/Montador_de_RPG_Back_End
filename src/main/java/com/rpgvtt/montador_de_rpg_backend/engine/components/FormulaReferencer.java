package com.rpgvtt.montador_de_rpg_backend.engine.components;

// import java.util.Map;
import tools.jackson.databind.JsonNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormulaReferencer {

    /**
     * Agora aceita Map<String, Object> vindo direto do JSONB do seu Personagem
     */
    public static void processarEExecutar(String formulaBruta, JsonNode atributosAtacante, JsonNode atributosAlvo) {
        
        Pattern pattern = Pattern.compile("@(atacante|alvo)\\.(\\w+)");
        Matcher matcher = pattern.matcher(formulaBruta);
        
        StringBuilder formulaNumerica = new StringBuilder();
        
        while (matcher.find()) {
            String escopo = matcher.group(1);   // "atacante" ou "alvo"
            String atributo = matcher.group(2); // "forca", "defesa", etc.
            
            // Busca o objeto genérico do mapa
            Object valorObj = "atacante".equals(escopo) 
                    ? atributosAtacante.get(atributo) 
                    : atributosAlvo.get(atributo);
            
            Double valor = 0.0;
            
            // Converte com segurança qualquer tipo de número (Integer, Double, Long) vindo do JSON
            if (valorObj instanceof Number) {
                valor = ((Number) valorObj).doubleValue();
            } else if (valorObj instanceof String) {
                try {
                    valor = Double.parseDouble((String) valorObj);
                } catch (NumberFormatException e) {
                    valor = 0.0;
                }
            }
            
            matcher.appendReplacement(formulaNumerica, String.valueOf(valor));
        }
        matcher.appendTail(formulaNumerica);
        
        // Chama o seu FormulaSolver que já está pronto
        FormulaSolver.execute(formulaNumerica.toString());
    }
}