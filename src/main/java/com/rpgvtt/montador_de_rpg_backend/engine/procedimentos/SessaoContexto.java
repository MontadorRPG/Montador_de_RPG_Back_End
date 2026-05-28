package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public class SessaoContexto {
    private final ConcurrentHashMap<Integer, Deque<ProcedimentoContexto>> pilhas
            = new ConcurrentHashMap<>();

    public void iniciarSessao(Integer idSessao) {
        pilhas.put(idSessao, new ArrayDeque<>());
    }

    /** Push a new procedure frame onto the session stack */
    public void empurrar(Long idSessao, ProcedimentoContexto estado) {
        pilhas.computeIfAbsent(idSessao, id -> new ArrayDeque<>())
                .push(estado);
    }

    /** Peek at the currently active frame without removing it */
    public ProcedimentoContexto frameAtivo(Integer idSessao) {
        Deque<ProcedimentoContexto> pilha = pilhas.get(idSessao);
        if (pilha == null || pilha.isEmpty())
            throw new IllegalStateException("Nenhum procedimento ativo na sessão " + idSessao);
        return pilha.peek();
    }

    /** Pop the completed frame and return the parent (null if stack is now empty) */
    public ProcedimentoContexto concluirFrame(Integer idSessao) {
        Deque<ProcedimentoContexto> pilha = pilhas.get(idSessao);
        pilha.pop(); // remove completed child
        return pilha.isEmpty() ? null : pilha.peek(); // return parent or null
    }

    public boolean temProcedimentoAtivo(Integer idSessao) {
        Deque<ProcedimentoContexto> pilha = pilhas.get(idSessao);
        return pilha != null && !pilha.isEmpty();
    }

    public int profundidade(Integer idSessao) {
        Deque<ProcedimentoContexto> pilha = pilhas.get(idSessao);
        return pilha == null ? 0 : pilha.size();
    }
}
