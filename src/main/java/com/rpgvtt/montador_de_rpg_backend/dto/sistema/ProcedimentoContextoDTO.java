package com.rpgvtt.montador_de_rpg_backend.dto.sistema;

import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto.Status;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import lombok.Value;

import java.util.List;
import java.util.Objects;

@Value
public class ProcedimentoContextoDTO {

    Status status;
    String nomeProcedimento;
    int etapaAtual;
    Object inputSolicitado;
    List<Object> resultadosCiclo;
    String erro;

    public static ProcedimentoContextoDTO from(ProcedimentoContexto ctx) {
        if (ctx == null) {
            return new ProcedimentoContextoDTO(Status.CONCLUIDO, null, 0, null, List.of(), null);
        }

        // Todos os dados do histórico deste ciclo
        List<Object> resultadosCiclo = ctx.getHistorico().stream()
                .map(ResultadoEtapa::dados)
                .filter(Objects::nonNull)
                .toList();

        // inputSolicitado: dados do último AGUARDANDO_INPUT no histórico
        // (funciona para AGUARDANDO_INPUT e também se status for ERRO após aguardar)
        Object inputSolicitado = ctx.getHistorico().stream()
                .filter(r -> r.tipo() == ResultadoEtapa.TipoResultado.AGUARDANDO_INPUT)
                .reduce((first, second) -> second) // pega o último
                .map(ResultadoEtapa::dados)
                .orElse(null);

        // Mensagem de erro: tenta pegar do histórico, depois do status ERRO
        String erro = null;
        if (ctx.getStatus() == Status.ERRO) {
            erro = ctx.getHistorico().stream()
                    .filter(r -> r.tipo() == ResultadoEtapa.TipoResultado.ERRO)
                    .reduce((first, second) -> second)
                    .map(ResultadoEtapa::mensagem)
                    .orElse("Erro desconhecido no procedimento");
        }

        return new ProcedimentoContextoDTO(
                ctx.getStatus(),
                ctx.getProcedimento().getNome(),
                ctx.getEtapaAtual(),
                inputSolicitado,
                resultadosCiclo,
                erro
        );
    }
}