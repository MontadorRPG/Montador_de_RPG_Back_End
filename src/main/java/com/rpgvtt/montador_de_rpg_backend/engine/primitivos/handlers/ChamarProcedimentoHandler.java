package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChamarProcedimentoHandler implements EtapaHandler {

    private final SessaoContexto sessaoCtx;
    private final ProcedimentoLoader loader;
    private final ObjectMapper mapper;

    @Override
    public String tipoEtapa() { return "CHAMAR_PROCEDIMENTO"; }

    @Override
    public ResultadoEtapa executar(EtapaProcedimento etapa,
                                   ProcedimentoContexto procedimentoCtx) {

        Map<String, Object> params = mapper.convertValue(etapa.getParametros_etapa(), new TypeReference<>() {});

        Long idFilho  = (Long) params.get("id_procedimento");
        String salvarEm  = (String) params.get("salvar_em");
        String escopoTipo = (String) params.getOrDefault("escopo", "HERDAR");

        EscopoInstancias escopoFilho = resolverEscopo(escopoTipo, params, procedimentoCtx);

        @SuppressWarnings("unchecked")
        List<String> passarContexto = (List<String>)
                params.getOrDefault("passar_contexto", List.of());

        Map<String, Object> ctxFilho = new HashMap<>();
        for (String chave : passarContexto) {
            Object val = procedimentoCtx.getContexto().get(chave);
            if (val != null) ctxFilho.put(chave, val);
        }

        ProcedimentoContexto filho = loader.carregar(
                LoadRequest.filho(idFilho, procedimentoCtx, escopoFilho, salvarEm, ctxFilho)
        );

        sessaoCtx.empurrar(procedimentoCtx.getIdSessao(), filho);

        // Signal to the engine: don't advance parent yet, execute child first
        return ResultadoEtapa.subProcedimentoIniciado(filho.getProcedimento().getNome());
    }

    /**
     * Resolves which instances the child procedure operates on.
     * "HERDAR"       → same scope as parent (most common for single-actor chains)
     * "NENHUMA"      → child needs no instance (setup/teardown procedures)
     * "UNICA"        → a specific instance, read from contexto key
     * "CONTEXTO_IDS" → list of IDs previously saved in parent contexto
     * "TODOS"        → all session participants
     */

    private EscopoInstancias resolverEscopo(String tipo,
                                            Map<String, Object> params,
                                            ProcedimentoContexto pai) {
        return switch (tipo) {
            case "HERDAR"       -> pai.getEscopo();
            case "NENHUMA"      -> EscopoInstancias.nenhuma();
            case "UNICA" -> {
                String chave = (String) params.get("escopo_contexto_chave");
                Long id   = (Long) pai.getContexto().get(chave);
                yield EscopoInstancias.unica(id);
            }
            case "CONTEXTO_IDS" -> {
                String chave = (String) params.get("escopo_contexto_chave");
                @SuppressWarnings("unchecked")
                List<Long> ids = (List<Long>) pai.getContexto().get(chave);
                yield EscopoInstancias.multiplas(ids != null ? ids : List.of());
            }
            case "TODOS" -> {
                List<Long> ids = pai.getParticipantes().stream()
                        .map(p -> p.getInstancia().getId())
                        .toList();
                yield EscopoInstancias.multiplas(ids);
            }
            default -> throw new IllegalArgumentException(
                    "Tipo de escopo desconhecido: " + tipo);
        };
    }
}
