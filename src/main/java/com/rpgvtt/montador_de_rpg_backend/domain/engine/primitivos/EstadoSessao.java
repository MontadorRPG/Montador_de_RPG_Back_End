// domain/engine/primitivos/EstadoSessao.java
package com.rpgvtt.montador_de_rpg_backend.domain.engine.primitivos;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.EfeitoAtivo;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.HistoricoAcoes;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

@Getter
public class EstadoSessao {

    private final Sessao sessao;

    private final Map<Long, EntidadeInstancia> entidades;

    private final List<EfeitoAtivo> efeitosParaAdicionar = new ArrayList<>();

    private final List<Long> efeitosParaRemover = new ArrayList<>();

    private final List<HistoricoAcoes> historicoGerado = new ArrayList<>();

    private final List<Long> eventosDisparados = new ArrayList<>();

    private final List<Suspensao> suspensoes = new ArrayList<>();

    // Variáveis de contexto geradas durante a execução
    private final Map<String, Object> variaveis = new HashMap<>();

    public EstadoSessao(Sessao sessao, List<EntidadeInstancia> entidades) {
        this.sessao = sessao;
        this.entidades = new HashMap<>();
        entidades.forEach(e -> this.entidades.put(e.getId(), e));
    }

    public void modificarAtributo(Long idEntidade, String caminho, Object valor) {
        EntidadeInstancia entidade = entidades.get(idEntidade);
        if (entidade == null) {
            throw new IllegalArgumentException(
                "Entidade %d não encontrada no estado da sessão".formatted(idEntidade)
            );
        }
        aplicarNoCaminho(entidade.getAtributosAtuais(), caminho, valor);
    }

    private void aplicarNoCaminho(JsonNode raiz, String caminho, Object valor) {
        String[] partes = caminho.split("\\.");
        ObjectNode atual = (ObjectNode) raiz;
        for (int i = 0; i < partes.length - 1; i++) {
            atual = (ObjectNode) atual.get(partes[i]);
            if (atual == null) {
                throw new IllegalArgumentException("Caminho '%s' não encontrado".formatted(caminho));
            }
        }
        String ultimoCampo = partes[partes.length - 1];
            if (valor instanceof Double d) atual.put(ultimoCampo, d);
            else if (valor instanceof Integer i) atual.put(ultimoCampo, i);
            else if (valor instanceof Boolean b) atual.put(ultimoCampo, b);
            else if (valor instanceof String s) atual.put(ultimoCampo, s);
            else atual.putPOJO(ultimoCampo, valor); // fallback
    }

    public void adicionarEfeito(EfeitoAtivo efeito) {
        efeitosParaAdicionar.add(efeito);
    }

    public void removerEfeito(Long idEfeito) {
        efeitosParaRemover.add(idEfeito);
    }

    public void registrarHistorico(HistoricoAcoes historico) {
        historicoGerado.add(historico);
    }

    public void dispararEvento(Long idEvento) {
        eventosDisparados.add(idEvento);
    }

    public void adicionarSuspensao(Suspensao suspensao) {
        suspensoes.add(suspensao);
    }

    public void guardarVariavel(String nome, Object valor) {
        variaveis.put(nome, valor);
    }

    public boolean estaSuspenso() {
        return !suspensoes.isEmpty();
    }
}