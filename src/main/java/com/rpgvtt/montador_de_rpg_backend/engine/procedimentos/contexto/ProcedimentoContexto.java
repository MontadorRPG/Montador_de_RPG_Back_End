package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto;

import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.EscopoInstancias;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class ProcedimentoContexto {

    private Procedimento procedimento;
    private List<EtapaProcedimento> etapas;
    private Sistema sistema;
    private List<Personagem> participantes;

    private Integer etapaAtual;
    private boolean aguardandoInput;
    private EtapaProcedimento etapaPendente;

    private Map<String, Object> contextoMap = new HashMap<>();

    @Getter(AccessLevel.NONE)
    private ContextoAccessor contexto;

    private EscopoInstancias escopo;

    private Long idSessao;
    public enum Status {EM_ANDAMENTO, CONCLUIDO, ERRO}
    private Status status;
    private List<ResultadoEtapa> historico = new ArrayList<>();
    private String retornoContexto;

    @PostConstruct // or set in constructor
    public void inicializarAcessor() {
        this.contexto = new ContextoAccessor(contextoMap);
    }

    public ContextoAccessor getContexto() {
        if (contexto == null) contexto = new ContextoAccessor(contextoMap);
        return contexto;
    }

    public void setContextoMap(Map<String, Object> map) {
        this.contextoMap = map;
        this.contexto = new ContextoAccessor(map);
    }

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

    public boolean semInstancias() {
        return escopo instanceof EscopoInstancias.Nenhuma;
    }

    public boolean temInstanciaUnica() {return escopo instanceof EscopoInstancias.Unica;}

    private int iteracaoAtual = 0;

    public void incrementarIteracao() { iteracaoAtual++;}

    public int indiceParaOrdem(int ordemAlvo) {
        for (int i = 0; i < etapas.size(); i++) {
            if (etapas.get(i).getOrdem() == ordemAlvo) return i;
        }
        throw new IllegalArgumentException(
                "Nenhuma etapa com ordem=" + ordemAlvo +
                        " no procedimento '" + procedimento.getNome() + "'" +
                        " — ordens disponíveis: " +
                        etapas.stream()
                                .map(e -> String.valueOf(e.getOrdem()))
                                .collect(Collectors.joining(", "))
        );
    }

}
