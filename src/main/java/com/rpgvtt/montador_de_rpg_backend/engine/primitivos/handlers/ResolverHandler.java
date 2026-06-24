package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.Resolucao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.HandlerRegistry;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.repository.mecanica.ResolucaoRepository;
import com.rpgvtt.montador_de_rpg_backend.service.mecanica.ResolutionEvaluator;
import com.rpgvtt.montador_de_rpg_backend.service.mecanica.ResolutionOutcome;

// import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
// @RequiredArgsConstructor
public class ResolverHandler implements EtapaHandler {

    private final ResolucaoRepository resolucaoRepo;
    private final ResolutionEvaluator evaluator;
    private final InstanciaResolver instanciaResolver;
    private final GerenciarItemHandler gerenciarItem;
    private final ChamarProcedimentoHandler chamarProc;
    private final ObjectMapper mapper;
    private final HandlerRegistry handlers;

    public ResolverHandler(
        ResolucaoRepository resolucaoRepo,
        ResolutionEvaluator evaluator,
        InstanciaResolver instanciaResolver,
        GerenciarItemHandler gerenciarItem,
        ChamarProcedimentoHandler chamarProc,
        @Lazy HandlerRegistry handlers,
        ObjectMapper mapper) {

        this.resolucaoRepo = resolucaoRepo;
        this.evaluator = evaluator;
        this.instanciaResolver = instanciaResolver;
        this.gerenciarItem = gerenciarItem;
        this.chamarProc = chamarProc;
        this.handlers = handlers;
        this.mapper = mapper;

    }

    @Override
    public String tipoEtapa() { return "RESOLVER"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();
        Long idResolucao = params.get("id_resolucao").asLong();
        String salvarResultadoEm = params.path("salvar_resultado_em").asString(null);

        Resolucao resolucao = resolucaoRepo.findById(idResolucao)
                .orElseThrow(() -> new IllegalArgumentException("Resolução não encontrada: " + idResolucao));

        // Se a resolução precisa de uma rolagem ainda não fornecida, solicita ao front
        String chaveRolagem = resolucao.getParametros().path("chave_rolagem").asString(null);
        if (chaveRolagem != null && !ctx.getContexto().containsKey(chaveRolagem)) {
            Map<String, Object> rolagemConfig = buildRolagemConfig(resolucao);
            return ResultadoEtapa.aguardandoInput(Map.of(
                "tipo_input", "ROLAGEM",
                "campoPedido", resolucao.getNome(),
                "salvar_em", chaveRolagem,
                "rolagem", rolagemConfig
            ));
        }

        // Constrói o contexto JSON a partir do estado atual
        JsonNode contextoJson = buildContextoJson(ctx);

        // Avalia a resolução
        ResolutionOutcome outcome = evaluator.evaluate(resolucao, contextoJson);

        if (salvarResultadoEm != null) {
            ctx.getContexto().put(salvarResultadoEm, outcome.success());
        }

        // Aplica ações da tabela, se houver
        return aplicarAcoes(outcome, resolucao, ctx);
    }

    private ResultadoEtapa aplicarAcoes(ResolutionOutcome outcome, Resolucao resolucao, ExecucaoContexto ctx) {
        JsonNode params = resolucao.getParametros();
        JsonNode tabela = params.get("tabela");
        if (tabela == null || !tabela.isArray()) {
            return ResultadoEtapa.concluida(Map.of("sucesso", outcome.success(), "motivo", outcome.motivo()));
        }

        // Localiza a entrada correspondente ao resultado
        Object rollValue = outcome.roll();
        JsonNode entrada = null;
        if (rollValue instanceof Integer roll) {
            entrada = findTabelaEntrada(tabela, roll);
        } else if (rollValue instanceof List<?> list && list.size() == 2) {
            int r1 = ((Number) list.get(0)).intValue();
            int r2 = ((Number) list.get(1)).intValue();
            entrada = findTabelaDuplaEntrada(tabela, r1, r2);
        }

        if (entrada == null) {
            return ResultadoEtapa.erro("Nenhuma entrada na tabela para o resultado " + rollValue);
        }

        if (entrada.has("id_condicao")) {
            Long idCondicao = entrada.get("id_condicao").asLong();
            Map<String, Object> itemParams = Map.of(
                "tipo", "CRIAR",
                "opcao_fonte_item", "estatico",
                "opcao_item", idCondicao.toString(),
                "fonte_qtd", "estatico",
                "opcao_qtd", "1",
                "opcao_fonte_personagem", "instancia_ativa"
            );
            EtapaProcedimento subEtapa = new EtapaProcedimento();
            subEtapa.setParametrosEtapa(mapper.valueToTree(itemParams));
            return gerenciarItem.executar(subEtapa, ctx);
        }

        if (entrada.has("id_procedimento")) {
            Long idProc = entrada.get("id_procedimento").asLong();
            Map<String, Object> procParams = Map.of(
                "id_procedimento", idProc,
                "salvar_em", "resultado_scar",
                "escopo", "HERDAR"
            );
            EtapaProcedimento subEtapa = new EtapaProcedimento();
            subEtapa.setParametrosEtapa(mapper.valueToTree(procParams));
            return chamarProc.executar(subEtapa, (ProcedimentoContexto) ctx);
        }

        if (entrada.has("acao")) {
            JsonNode acao = entrada.get("acao");
            String tipoEtapa = acao.get("tipoEtapa").asString();
            JsonNode paramsEtapa = acao.get("parametros");
            
            EtapaProcedimento etapaDinamica = new EtapaProcedimento();
            etapaDinamica.setTipoEtapa(tipoEtapa);
            etapaDinamica.setParametrosEtapa(paramsEtapa);
            
            EtapaHandler handler = handlers.get(tipoEtapa);  // injetar HandlerRegistry
            if (handler == null) return ResultadoEtapa.erro("Handler não encontrado: " + tipoEtapa);
            
            return handler.executar(etapaDinamica, ctx);
        }

        return ResultadoEtapa.concluida(Map.of("sucesso", true));
    }

    private JsonNode buildContextoJson(ExecucaoContexto ctx) {
        Map<String, Object> map = new HashMap<>();
        for (String key : ctx.getContexto().keySet()) {
            ctx.getContexto().get(key, Object.class).ifPresent(val -> map.put(key, val));
        }
        if (ctx.temInstanciaUnica()) {
            EntidadeInstancia inst = instanciaResolver.retornarAtiva(ctx);
            map.put("atributos", inst.getAtributosAtuais());
        }
        return mapper.valueToTree(map);
    }

    private Map<String, Object> buildRolagemConfig(Resolucao resolucao) {
        JsonNode params = resolucao.getParametros();
        String tipoRolagem = params.path("tipo_rolagem").asString("dado_unico");
        JsonNode dadosNode = params.get("dados");
        List<String> dados = new ArrayList<>();
        if (dadosNode != null && dadosNode.isArray()) {
            for (JsonNode d : dadosNode) dados.add(d.asString());
        } else {
            dados.add("d20");
        }
        return Map.of("dados", dados);
    }

    // Métodos de busca duplicados para evitar dependência circular; podem ser movidos para um utilitário.
    private JsonNode findTabelaEntrada(JsonNode tabela, int valor) {
        if (tabela == null || !tabela.isArray()) return null;
        for (JsonNode entry : tabela) {
            if (entry.has("min") && entry.has("max")) {
                if (valor >= entry.get("min").asInt() && valor <= entry.get("max").asInt()) return entry;
            } else if (entry.has("valor") && entry.get("valor").asInt() == valor) return entry;
        }
        return null;
    }

    private JsonNode findTabelaDuplaEntrada(JsonNode tabela, int roll1, int roll2) {
        if (tabela == null || !tabela.isArray()) return null;
        for (JsonNode entry : tabela) {
            if (entry.has("d6") && entry.has("d12")) {
                if (entry.get("d6").asInt() == roll1 && entry.get("d12").asInt() == roll2) return entry;
            } else if (entry.has("valor1") && entry.has("valor2")) {
                if (entry.get("valor1").asInt() == roll1 && entry.get("valor2").asInt() == roll2) return entry;
            }
        }
        return null;
    }
}