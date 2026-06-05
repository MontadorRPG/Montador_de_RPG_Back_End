package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.components.InterpretadorJson;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.ContextoJsonNode;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.ResultadoExpressao;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CalcularAtributoHandler implements EtapaHandler {

    private final InterpretadorJson interpretadorJson;
    private final JsonMapper mapper;
    private final InstanciaResolver instanciaResolver;
    private final EntidadeInstanciaRepository instanciaRepo;

    @Override
    public String tipoEtapa() {
        return "CALCULAR_ATRIBUTO";
    }

    @Override
    public ResultadoEtapa executar(EtapaProcedimento etapa, ProcedimentoContexto ctx) {

        Map<String, Object> params = mapper.convertValue(etapa.getParametros_etapa(), new TypeReference<>() {});
        String sourceKey = params.get("source_key").toString();
        String ctxKey = params.get("salvar_em").toString();
        JsonNode expressao = (JsonNode) params.get("expressao");
        Long idEntidade = (Long) params.get("id_entidade"); // opcional

        EntidadeInstancia inst;

        if (idEntidade == null) {
            inst = instanciaResolver.retornarAtiva(ctx);
        } else {
            inst = instanciaRepo.findById(idEntidade)
                    .orElseThrow(() -> new EntityNotFoundException(EntidadeInstancia.class, idEntidade));
        }

        Map<String, Object> execCtx = new HashMap<>();

        Integer resultadoAnterior = (Integer) ctx.getContexto().get(sourceKey);

        execCtx.put("atributos", inst.getAtributosAtuais());
        execCtx.put("resultado", resultadoAnterior);

        ContextoJsonNode ctxFinal = new ContextoJsonNode(mapper.valueToTree(execCtx));

        double val = interpretadorJson.interpretar(expressao, ctxFinal).comoNumero();

        ctx.getContexto().put(ctxKey, val);

        return null;
    }
}
