package com.rpgvtt.montador_de_rpg_backend.domain.engine.utils;


import java.util.Optional;
import java.util.Map;

public class ContextoComposto implements Contexto {

    private final Map<String, Contexto> escopos;

    public ContextoComposto(Map<String, Contexto> escopos) {
        this.escopos = escopos;
    }

    @Override
    public Optional<Object> get(String caminho) {
        // "atacante.for.modificador" → escopo "atacante", caminho "for.modificador"
        int ponto = caminho.indexOf(".");
        if (ponto == -1) return Optional.empty();

        String escopo = caminho.substring(0, ponto);
        String resto  = caminho.substring(ponto + 1);

        Contexto ctx = escopos.get(escopo);
        if (ctx == null) return Optional.empty();

        return ctx.get(resto);
    }
}
