package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto;

import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EscopoInstancias;

import java.util.List;
import java.util.Map;

public record LoadRequest(
        Long idProcedimento,
        Long idSessao,
        EscopoInstancias escopo,
        String               retornoContexto,      // null for root procedures
        Map<String, Object> contextoInicial,      // keys to seed from parent
        List<Personagem> participantesHerdados // null = query DB
) {
    public LoadRequest {
        contextoInicial = contextoInicial != null ? contextoInicial : Map.of();
    }


    public static LoadRequest raiz(Long idProcedimento,
                                   Long idSessao,
                                   EscopoInstancias escopo) {
        return new LoadRequest(
                idProcedimento, idSessao, escopo,
                null, null, null
        );
    }

    public static LoadRequest raiz(Long idProcedimento,
                                   Long idSessao,
                                   EscopoInstancias escopo,
                                   Map<String, Object> contextoInicial) {
        return new LoadRequest(
                idProcedimento, idSessao, escopo,
                null, contextoInicial, null
        );
    }

    public static LoadRequest semInstancia(Long idProcedimento,
                                           Long idSessao) {
        return new LoadRequest(
                idProcedimento, idSessao,
                EscopoInstancias.nenhuma(),
                null, null, null
        );
    }

    public static LoadRequest filho(Long idProcedimento,
                                    ProcedimentoContexto paiCtx,
                                    EscopoInstancias escopo,
                                    String retornoContexto,
                                    Map<String, Object> contextoInicial) {
        return new LoadRequest(
                idProcedimento,
                paiCtx.getIdSessao(),
                escopo,
                retornoContexto,
                contextoInicial,
                paiCtx.getParticipantes() // reuse — no DB call
        );
    }
}
