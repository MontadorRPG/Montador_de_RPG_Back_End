package com.rpgvtt.montador_de_rpg_backend.domain.engine.utils;

import java.util.List;
import java.util.Map;

public class ResultadoExpressao {

    public enum TipoResultado {
        NUMERO, BOOLEANO, TEXTO, ALVO, INSTRUCAO, LISTA, OBJETO, NULO
    }

    private final TipoResultado tipo;
    private final Object valor;

    private ResultadoExpressao(TipoResultado tipo, Object valor) {
        this.tipo = tipo;
        this.valor = valor;
    }

    public static ResultadoExpressao numero(double valor) {
        return new ResultadoExpressao(TipoResultado.NUMERO, valor);
    }

    public static ResultadoExpressao booleano(boolean valor) {
        return new ResultadoExpressao(TipoResultado.BOOLEANO, valor);
    }

    public static ResultadoExpressao texto(String valor) {
        return new ResultadoExpressao(TipoResultado.TEXTO, valor);
    }

    public static ResultadoExpressao alvo(Alvo alvo) {
        return new ResultadoExpressao(TipoResultado.ALVO, alvo);
    }

    public static ResultadoExpressao instrucao(Object expressao) {
        return new ResultadoExpressao(TipoResultado.INSTRUCAO, expressao);
    }

    public static ResultadoExpressao nulo() {
        return new ResultadoExpressao(TipoResultado.NULO, null);
    }

    public static ResultadoExpressao lista(List<?> valor) {
        return new ResultadoExpressao(TipoResultado.LISTA, valor);
    }

    public static ResultadoExpressao objeto(Object valor) {
        return new ResultadoExpressao(TipoResultado.OBJETO, valor);
    }

    // Getters com verificação
    public double comoNumero() {
        if (tipo != TipoResultado.NUMERO)
            throw new IllegalStateException("Resultado é " + tipo + ", não NUMERO");
        return (double) valor;
    }

    public boolean comoBooleano() {
        if (tipo != TipoResultado.BOOLEANO)
            throw new IllegalStateException("Resultado é " + tipo + ", não BOOLEANO");
        return (boolean) valor;
    }

    public String comoTexto() {
        if (tipo != TipoResultado.TEXTO)
            throw new IllegalStateException("Resultado é " + tipo + ", não TEXTO");
        return (String) valor;
    }

    public List<?> comoLista() {
        if (tipo != TipoResultado.LISTA)
            throw new IllegalStateException("Resultado é " + tipo + ", não LISTA");
        return (List<?>) valor;
    }

    public Alvo comoAlvo() {
        if (tipo != TipoResultado.ALVO)
            throw new IllegalStateException("Resultado é " + tipo + ", não ALVO");
        return (Alvo) valor;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> comoInstrucao() {
        if (tipo != TipoResultado.INSTRUCAO)
            throw new IllegalStateException("Resultado é " + tipo + ", não INSTRUCAO");
        return (Map<String, Object>) valor;
    }

    public Object comoObjeto() {
        if (tipo != TipoResultado.OBJETO)
            throw new IllegalStateException("Resultado é " + tipo + ", não OBJETO");
        return (Object) valor;
    }


    public TipoResultado getTipo() { return tipo; }

    
    public Object getValor() { return valor; }

    @Override
    public String toString() {
        return "ResultadoExpressao{tipo=" + tipo + ", valor=" + valor + "}";
    }
}