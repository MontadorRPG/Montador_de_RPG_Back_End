package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EscopoInstancias;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// In DB:
// {
//   "fonte":           "contexto.ids_inimigos",
//   "sub_etapa_tipo":  "VERIFICAR_CONDICAO",
//   "sub_etapa_params": { "atributo": "hp", "operador": "MAIOR_QUE", "valor": 0 },
//   "salvar_ids_em":   "ids_inimigos_vivos",  // IDs that passed the check
//   "salvar_count_em": "count_inimigos_vivos"
// }
@Component
@RequiredArgsConstructor
public class ForEachHandler implements EtapaHandler {

    private final InstanciaResolver instanciaResolver;
    private final Map<String, EtapaHandler> handlers;

    @Override
    public String tipoEtapa() { return "PARA_CADA"; }

    private final JsonMapper mapper;

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        Map<String, Object> params = mapper.convertValue(etapa.getParametrosEtapa(), new TypeReference<>() {});

        String fonte          = (String) params.get("fonte");
        String subTipo        = (String) params.get("sub_etapa_tipo");
        String salvarIdsEm    = (String) params.get("salvar_ids_em");
        String salvarCountEm  = (String) params.get("salvar_count_em");

        @SuppressWarnings("unchecked")
        Map<String, Object> subParams = (Map<String, Object>)
                params.getOrDefault("sub_etapa_params", Map.of());

        List<EntidadeInstancia> instancias = instanciaResolver.resolverDeFonte(fonte, ctx);

        EtapaHandler subHandler = handlers.get(subTipo);
        if (subHandler == null) throw new IllegalArgumentException(
                "Sub-handler não encontrado: " + subTipo);

        // Build a minimal EtapaProcedimento to pass to the sub-handler
        EtapaProcedimento subEtapa = new EtapaProcedimento();
        subEtapa.setTipoEtapa(subTipo);
        subEtapa.setParametrosEtapa(mapper.valueToTree(subParams));

        List<Long> idsPasaram = new ArrayList<>();
        Map<Long, Object> resultadosPorId = new LinkedHashMap<>();

        for (EntidadeInstancia instancia : instancias) {
            // Temporarily set scope to this single instance for the sub-handler
            EscopoInstancias escopoOriginal = ctx.getEscopo();
            ctx.setEscopo(EscopoInstancias.unica(instancia.getId()));

            ResultadoEtapa resultado = subHandler.executar(subEtapa, ctx);

            ctx.setEscopo(escopoOriginal); // restore

            resultadosPorId.put(instancia.getId(), resultado.dados());

            if (resultado.tipo() == ResultadoEtapa.Tipo.CONCLUIDA) {
                idsPasaram.add(instancia.getId());
            }
        }

        // Save results for downstream handlers
        if (salvarIdsEm != null) {
            ctx.getContexto().put(salvarIdsEm, idsPasaram);
        }
        if (salvarCountEm != null) {
            ctx.getContexto().put(salvarCountEm, (long) idsPasaram.size());
        }

        return ResultadoEtapa.concluida(Map.of(
                "total",         instancias.size(),
                "passaram",      idsPasaram.size(),
                "resultados",    resultadosPorId
        ));
    }
}
