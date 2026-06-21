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
        if (ctx == null) return new ProcedimentoContextoDTO(Status.CONCLUIDO, null, 0, null, List.of(), null);

        List<Object> resultadosCiclo = ctx.getHistorico().stream()
                .map(ResultadoEtapa::dados)
                .filter(Objects::nonNull)
                .toList();

        boolean aguardando = ctx.getStatus() == Status.AGUARDANDO_INPUT
                || ctx.getStatus() == Status.AGUARDANDO_INPUT_MULTIPLO ;

        Object inputSolicitado = null;
        if (aguardando && ctx.getEtapaPendente() != null) {
            inputSolicitado = ctx.getHistorico().stream()
                    .filter(r -> r.tipo() == ResultadoEtapa.Tipo.AGUARDANDO_INPUT
                            || r.tipo() == ResultadoEtapa.Tipo.AGUARDANDO_INPUT_MULTIPLO)
                    .reduce((first, second) -> second)
                    .map(ResultadoEtapa::dados)
                    .orElse(null);
        }

        return new ProcedimentoContextoDTO(
                ctx.getStatus(),
                ctx.getProcedimento().getNome(),
                ctx.getEtapaAtual(),
                inputSolicitado,
                resultadosCiclo,
                ctx.getStatus() == Status.ERRO
                        ? ctx.getHistorico().getLast().mensagem()
                        : null
        );
    }
}