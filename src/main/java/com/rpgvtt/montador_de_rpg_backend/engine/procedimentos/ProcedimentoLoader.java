package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.EtapaProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.ProcedimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcedimentoLoader {

    private ProcedimentoRepository procedimentoRepo;
    private EtapaProcedimentoRepository etapaProcedimentoRepo;

    public ProcedimentoContexto carregar(Long idProcedimento, Long idSessao, Long idInstanciaAtiva) {
        ProcedimentoContexto procedimentoCtx = new ProcedimentoContexto();
        procedimentoCtx.setIdSessao(idSessao);
        procedimentoCtx.setIdInstanciaAtiva(idInstanciaAtiva);

        Procedimento procedimento = procedimentoRepo.findById(idProcedimento)
                .orElseThrow(() -> new EntityNotFoundException(Procedimento.class, idProcedimento));

        List<EtapaProcedimento> etapas = etapaProcedimentoRepo.findByIdProcedimentoOrderByOrdem(idProcedimento);
        etapas.sort(Comparator.comparing(EtapaProcedimento::getOrdem));

        procedimentoCtx.setProcedimento(procedimento);
        procedimentoCtx.setEtapas(etapas);
        procedimentoCtx.setSistema(procedimento.getSistema());
        procedimentoCtx.setParticipantes(procedimento.get);



        return procedimentoCtx;
    }
}
