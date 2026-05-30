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

    private Procedimento procedimento;
    private List<EtapaProcedimento> etapas;
    private Sistema sistema;
    private List<Personagem> participantes;

    private Integer etapaAtual;
    private boolean aguardandoInput;
    private EtapaProcedimento etapaPendente;
    private Map<String, Object> contexto = new HashMap<>(); // Example keys: "acao_escolhida", "iniciativa_resultado",

    private EscopoInstancias escopo;

    private Long idSessao;
    public enum Status {EM_ANDAMENTO, CONCLUIDO, ERRO}
    private Status status;
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

    public void avancarEtapa()  { etapaAtual++; aguardandoInput = false; }

    public void pularEtapa(String motivo) {
        historico.add(ResultadoEtapa.pulada(motivo));
        etapaAtual++;
    }

    public List<Long> idsInstancias() {
        return switch (escopo) {
            case EscopoInstancias.Nenhuma n      -> List.of();
            case EscopoInstancias.Unica u        -> List.of(u.id());
            case EscopoInstancias.Multiplas m    -> m.ids();
        };
    }

    public Long idInstanciaAtiva() {
        if (escopo instanceof EscopoInstancias.Unica u) return u.id();
        throw new IllegalStateException(
                "Procedimento '" + procedimento.getNome() + "' não tem escopo UNICA" +
                        " — use idsInstancias() para escopo " + escopo.getClass().getSimpleName());
    }
}
