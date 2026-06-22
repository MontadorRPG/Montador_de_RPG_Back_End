package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ContextoAccessor;

import java.util.List;

public interface ExecucaoContexto {
    ContextoAccessor getContexto();
    Sistema getSistema();
    Long getIdSistema();
    EscopoInstancias getEscopo();

    void setEscopo(EscopoInstancias escopo);

    boolean semInstancias();
    boolean temInstanciaUnica();
    Long idInstanciaAtiva();
    List<Long> idsInstancias();
    Procedimento getProcedimento();
}
