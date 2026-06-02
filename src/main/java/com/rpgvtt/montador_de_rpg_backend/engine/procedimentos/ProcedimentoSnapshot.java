package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.ProcedimentoContexto.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcedimentoSnapshot {

    private Long idProcedimento;
    private Long idSessao;
    private int etapaAtual;
    private Status status;
    private String contextoRetorno;

    private String escopoTipo;
    private List<Long> escopoIds;

    private Map<String, Object> contexto = new HashMap<>();

    private List<ResultadoEtapa> historico = new ArrayList<>();
}