package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.SessaoContexto;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.EtapaProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.ProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.SistemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChamarProcedimentoHandler implements EtapaHandler {

    private final ProcedimentoRepository procedimentoRepo;
    private final EtapaProcedimentoRepository etapaRepo;
    private final SistemaRepository sistemaRepo;
    private final SessaoContexto sessaoCtx;

    @Override
    public String tipoEtapa() { return "CHAMAR_PROCEDIMENTO"; }

    @Override
    public ResultadoEtapa executar(EtapaProcedimento etapa,
                                   ProcedimentoContexto procedimentoCtx,
                                   Map<String, Object> input) {

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> params = mapper.convertValue(etapa.getParametros_etapa(), new TypeReference<Map<String, Object>>(){});
        Long idFilho  = (Long) params.get("id_procedimento");
        String salvarEm  = (String) params.get("salvar_em");

        @SuppressWarnings("unchecked")
        List<String> passarContexto = (List<String>)
                params.getOrDefault("passar_contexto", List.of());

        // Build the child frame
        Procedimento procFilho = procedimentoRepo.findById(idFilho).orElseThrow();
        List<EtapaProcedimento> etapasFilho = etapaRepo
                .findByIdProcedimentoOrderByOrdem(idFilho);
        etapasFilho.sort(Comparator.comparing(EtapaProcedimento::getOrdem));

        Sistema sistemaFilho = procedimentoCtx.getSistema();

        ProcedimentoContexto procedimentoFilhoCtx = new ProcedimentoContexto();
        procedimentoFilhoCtx.setProcedimento(procFilho);
        procedimentoFilhoCtx.setEtapas(etapasFilho);
        procedimentoFilhoCtx.setSistema(sistemaFilho);
        procedimentoFilhoCtx.setParticipantes(procedimentoCtx.getParticipantes());
        procedimentoFilhoCtx.setIdInstanciaAtiva(procedimentoCtx.getIdInstanciaAtiva());
        procedimentoFilhoCtx.setIdSessao(procedimentoCtx.getIdSessao());
        procedimentoFilhoCtx.setEtapaAtual(0);
        procedimentoFilhoCtx.setStatus("EM_ANDAMENTO");
        procedimentoFilhoCtx.setRetornoContexto(salvarEm); // where to write result in parent

        // Copy requested keys from parent contexto into child
        Map<String, Object> ctxFilho = new HashMap<>();
        passarContexto.forEach(chave -> {
            Object val = procedimentoCtx.getContexto().get(chave);
            if (val != null) ctxFilho.put(chave, val);
        });
        procedimentoFilhoCtx.setContexto(ctxFilho);

        // Push child onto the stack — engine will execute it next
        sessaoCtx.empurrar(procedimentoCtx.getIdSessao(), procedimentoFilhoCtx);

        // Signal to the engine: don't advance parent yet, execute child first
        return ResultadoEtapa.subProcedimentoIniciado(procFilho.getNome());
    }
}
