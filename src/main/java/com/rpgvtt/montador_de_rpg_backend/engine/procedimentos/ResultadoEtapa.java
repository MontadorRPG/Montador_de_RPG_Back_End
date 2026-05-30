package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

public record ResultadoEtapa(
    TipoResultado tipo,
    String mensagem,
    Object dados, // qualquer payload para o frontend
    boolean skipProximo
) {
    public enum TipoResultado { CONCLUIDA, AGUARDANDO_INPUT, PULADA, SUB_PROCEDIMENTO_INICIADO, ERRO}

    public static ResultadoEtapa concluida(Object dados) {
        return new ResultadoEtapa(TipoResultado.CONCLUIDA, null, dados, false);
    }
    public static ResultadoEtapa aguardandoInput(Object prompt) {
        return new ResultadoEtapa(TipoResultado.AGUARDANDO_INPUT, null, prompt, false);
    }
    public static ResultadoEtapa pulada(String motivo) {
        return new ResultadoEtapa(TipoResultado.PULADA, motivo, null, false);
    }
    public static ResultadoEtapa erro(String msg) {
        return new ResultadoEtapa(TipoResultado.ERRO, msg, null, false);
    }
}
