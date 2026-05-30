package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessaoContexto {
    private final ConcurrentHashMap<Long, Deque<ProcedimentoContexto>> pilhas
            = new ConcurrentHashMap<>();

    public void iniciarSessao(Long idSessao) {
        pilhas.put(idSessao, new ArrayDeque<>());
    }

    /** Push a new procedure frame onto the session stack */
    public void empurrar(Long idSessao, ProcedimentoContexto ctx) {
        pilhas.computeIfAbsent(idSessao, _ -> new ArrayDeque<>())
                .push(ctx);
    }

    /** Peek at the currently active frame without removing it */
    public ProcedimentoContexto frameAtivo(Long idSessao) {
        Deque<ProcedimentoContexto> pilha = pilhas.get(idSessao);
        if (pilha == null || pilha.isEmpty())
            throw new IllegalStateException("Nenhum procedimento ativo na sessão " + idSessao);
        return pilha.peek();
    }

    /** Pop the completed frame and return the parent (null if stack is now empty) */
    public ProcedimentoContexto concluirFrame(Long idSessao) {
        Deque<ProcedimentoContexto> pilha = pilhas.get(idSessao);
        pilha.pop(); // remove completed child
        return pilha.isEmpty() ? null : pilha.peek(); // return parent or null
    }

    public boolean temProcedimentoAtivo(Long idSessao) {
        Deque<ProcedimentoContexto> pilha = pilhas.get(idSessao);
        return pilha != null && !pilha.isEmpty();
    }

    public int profundidade(Long idSessao) {
        Deque<ProcedimentoContexto> pilha = pilhas.get(idSessao);
        return pilha == null ? 0 : pilha.size();
    }
}
