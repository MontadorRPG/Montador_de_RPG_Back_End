// domain/engine/primitivos/executores/ModificarAtributoExecutor.java
package com.rpgvtt.montador_de_rpg_backend.domain.engine.primitivos.executores;

import com.rpgvtt.montador_de_rpg_backend.domain.engine.components.InterpretadorJson;
import com.rpgvtt.montador_de_rpg_backend.domain.engine.primitivos.EstadoSessao;
import com.rpgvtt.montador_de_rpg_backend.domain.engine.primitivos.PrimitivoExecutor;
import com.rpgvtt.montador_de_rpg_backend.domain.engine.utils.Contexto;
import com.rpgvtt.montador_de_rpg_backend.domain.engine.utils.ResultadoExpressao;
import tools.jackson.databind.JsonNode;

// Primitivo: modifica um atributo de uma entidade
public class ModificarAtributoExecutor implements PrimitivoExecutor {

    private final InterpretadorJson interpretador;

    public ModificarAtributoExecutor(InterpretadorJson interpretador) {
        this.interpretador = interpretador;
    }

    @Override
    public void executar(JsonNode parametros, Contexto contexto, EstadoSessao estado) {

        // Resolve o id da entidade alvo
        JsonNode idAlvoNode = parametros.get("id_alvo");
        if (idAlvoNode == null) {
            throw new IllegalArgumentException("modificar_atributo requer 'id_alvo'");
        }
        double idAlvo = interpretador.interpretar(idAlvoNode, contexto).comoNumero();

        // Pega o caminho
        JsonNode caminhoNode = parametros.get("caminho");
        if (caminhoNode == null || !caminhoNode.isString()) {
            throw new IllegalArgumentException("modificar_atributo requer 'caminho' como string");
        }
        String caminho = caminhoNode.asString();

        // Resolve o valor
        JsonNode valorNode = parametros.get("valor");
        if (valorNode == null) {
            throw new IllegalArgumentException("modificar_atributo requer 'valor'");
        }
        ResultadoExpressao resultado = interpretador.interpretar(valorNode, contexto);

        estado.modificarAtributo((long) idAlvo, caminho, resultado.getValor());
    }
}