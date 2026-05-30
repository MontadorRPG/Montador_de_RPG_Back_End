package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import java.util.List;

// The three possible scopes as a sealed hierarchy
public sealed interface EscopoInstancias
        permits EscopoInstancias.Nenhuma,
        EscopoInstancias.Unica,
        EscopoInstancias.Multiplas {

    record Nenhuma() implements EscopoInstancias {}

    record Unica(Long idInstancia) implements EscopoInstancias {
        public Long id() { return idInstancia; }
    }

    record Multiplas(List<Long> ids) implements EscopoInstancias {
        // ordered — matters for sequential area resolution
        public Multiplas(List<Long> ids) {
            this.ids = List.copyOf(ids);
        }
    }

    // Factory methods for call sites
    static EscopoInstancias nenhuma() { return new Nenhuma(); }
    static EscopoInstancias unica(Long id) { return new Unica(id); }
    static EscopoInstancias multiplas(List<Long> ids) { return new Multiplas(ids); }
}
