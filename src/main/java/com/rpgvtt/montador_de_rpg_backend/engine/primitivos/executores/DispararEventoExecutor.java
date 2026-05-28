package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.executores;

import com.rpgvtt.montador_de_rpg_backend.engine.components.InterpretadorJson;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.EstadoSessao;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.PrimitivoExecutor;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.Contexto;
import com.fasterxml.jackson.databind.JsonNode;

// Primitivo: dispara um EventoSistema para ser processado após os primitivos
public class DispararEventoExecutor implements PrimitivoExecutor {

    private final InterpretadorJson interpretador;

    public DispararEventoExecutor(InterpretadorJson interpretador) {
        this.interpretador = interpretador;
    }

    @Override
    public void executar(JsonNode parametros, Contexto contexto, EstadoSessao estado) {

        JsonNode idEventoNode = parametros.get("id_evento");
        if (idEventoNode == null) {
            throw new IllegalArgumentException("disparar_evento requer 'id_evento'");
        }

        long idEvento = (long) interpretador.interpretar(idEventoNode, contexto).comoNumero();
        estado.dispararEvento(idEvento);
    }
}