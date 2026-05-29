package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ProcedimentoContexto {

    // ── Structural data: load once, never mutate ──────────────────
    // These never change during a procedure execution.
    // Loading them once is both correct and a genuine optimization.

    private Procedimento procedimento;              // includes tipo, configs_geral
    private List<EtapaProcedimento> etapas;         // pre-sorted by ordem
    private Sistema sistema;                        // includes configs_geral
    private List<Personagem> participantes;         // display info, features

    // ── Execution cursor ──────────────────────────────────────────
    private Integer etapaAtual;                         // index into etapas list
    private boolean aguardandoInput;
    private EtapaProcedimento etapaPendente;

    // Example keys: "acao_escolhida", "iniciativa_resultado",
    //               "resultado_ataque", "dano_final", "ordem_turnos"

    private Map<String, Object> contexto = new HashMap<>();

    private Long idInstanciaAtiva;
    private Long idSessao;

    private enum status {EM_ANDAMENTO, CONCLUIDO, ERRO}
    private List<ResultadoEtapa> historico = new ArrayList<>();

    private String retornoContexto;

    public EtapaProcedimento etapaCorrente() {
        return etapas.get(etapaAtual);
    }

    public Long getIdSistema() {
        return sistema.getId();
    }

    public boolean etapasConcluidas() {
        return etapaAtual >= etapas.size();
    }
}
