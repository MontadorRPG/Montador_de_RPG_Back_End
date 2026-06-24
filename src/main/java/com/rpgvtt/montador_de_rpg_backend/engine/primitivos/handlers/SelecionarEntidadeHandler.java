package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeSistemaRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class SelecionarEntidadeHandler implements EtapaHandler {

    private final EntidadeSistemaRepository repo;

    @Override
    public String tipoEtapa() { return "SELECIONAR_ENTIDADE"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();
        String tipo = params.get("tipo_entidade").asString();
        String salvarEm = params.get("salvar_em").asString();

        if (ctx.getContexto().containsKey(salvarEm)) {
            Long id = ctx.getContexto().getLongOrThrow(salvarEm);
            return ResultadoEtapa.concluida(Map.of(salvarEm, id));
        }

        List<EntidadeSistema> entidades = repo.findBySistemaIdAndTipo(ctx.getIdSistema(), tipo);

        List<Map<String, Object>> opcoes = new ArrayList<>();
        for (EntidadeSistema e : entidades) {
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("label", e.getNome());
            op.put("valor", e.getId());
            opcoes.add(op);
        }

        return ResultadoEtapa.aguardandoInput(Map.of(
            "campoPedido", "Escolha um cavaleiro",
            "salvar_em", salvarEm,
            "opcoes", opcoes
        ));
    }
}
