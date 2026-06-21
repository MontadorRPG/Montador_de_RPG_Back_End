package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto;

/**
 * The outcome of executing a single etapa/primitivo. This is the only return
 * type EVERY EtapaHandler.executar(...) produces — ProcedimentoEngine.avancar()
 * switches exhaustively on tipo() to decide what happens next. Any new Tipo
 * added here must get a matching branch in avancar(), or the switch won't
 * compile (Java enforces exhaustiveness on enum switch expressions) — that
 * safety net is exactly what should have caught the previous gap.
 */
public record ResultadoEtapa(
        Tipo tipo,
        String mensagem,
        Object dados,
        Integer pularQuantidade   // only meaningful for Tipo.PULAR_ETAPAS
) {
    public enum Tipo {
        CONCLUIDA,
        AGUARDANDO_INPUT,
        AGUARDANDO_INPUT_MULTIPLO,
        SUB_PROCEDIMENTO_INICIADO,
        REPETINDO,
        PULAR_ETAPAS,
        ERRO
    }

    public static ResultadoEtapa concluida(Object dados) {
        return new ResultadoEtapa(Tipo.CONCLUIDA, null, dados, null);
    }

    public static ResultadoEtapa aguardandoInput(Object prompt) {
        return new ResultadoEtapa(Tipo.AGUARDANDO_INPUT, null, prompt, null);
    }

    public static ResultadoEtapa aguardandoInputMultiplo(Object prompt) {
        return new ResultadoEtapa(Tipo.AGUARDANDO_INPUT_MULTIPLO, null, prompt, null);
    }

    public static ResultadoEtapa subProcedimentoIniciado(String nomeProcedimento) {
        return new ResultadoEtapa(Tipo.SUB_PROCEDIMENTO_INICIADO, nomeProcedimento, null, null);
    }

    public static ResultadoEtapa repetindo(Object dados) {
        return new ResultadoEtapa(Tipo.REPETINDO, null, dados, null);
    }

    public static ResultadoEtapa pularEtapas(int quantidade, Object dados) {
        return new ResultadoEtapa(Tipo.PULAR_ETAPAS, null, dados, quantidade);
    }

    public static ResultadoEtapa pulada(String motivo) {
        return new ResultadoEtapa(Tipo.CONCLUIDA, motivo, null, null);
    }

    public static ResultadoEtapa erro(String mensagem) {
        return new ResultadoEtapa(Tipo.ERRO, mensagem, null, null);
    }
}