package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.components.InterpretadorJson;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InterpretadorContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaRepository;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SolicitadorRolagemHandler implements EtapaHandler {

    private final InterpretadorJson interpretador;
    // Dependências necessárias para construir o InterpretadorContexto
    private final InstanciaResolver instanciaResolver;
    private final CenaRepository cenaRepo;
    private final JsonMapper mapper;

    @Override
    public String tipoEtapa() { return "SOLICITAR_ROLAGEM"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();

        String campoPedido = params.path("campo_pedido").asString("Role os dados");
        boolean podePassar = params.path("pode_passar").asBoolean(false);
        String salvarEm = exigirTexto(params, "salvar_em", etapa);
        
        if (ctx.getContexto().containsKey(salvarEm)) {
            Object valor = ctx.getContexto().get(salvarEm, Object.class).orElse(null);
            return ResultadoEtapa.concluida(Map.of(
                "campoPedido", campoPedido,
                "escolha", valor != null ? valor.toString() : "",
                "rolos", ctx.getContexto().get(salvarEm + "_rolos", Object.class).orElse(List.of())
            ));
        }

        String formula = resolverFormula(params, ctx, etapa);
        Map<String, Object> rolagemConfig = parseFormula(formula);

        return ResultadoEtapa.aguardandoInput(Map.of(
            "campoPedido", campoPedido,
            "salvar_em", salvarEm,
            "pode_passar", podePassar,
            "rolagem", rolagemConfig
        ));
    }

    private String resolverFormula(JsonNode params, ExecucaoContexto ctx, EtapaExecutavel etapa) {
        // 1. Fórmulas estáticas (ex: "1d4")
        if (params.hasNonNull("formula")) {
            return params.get("formula").asString();
        }

        // 2. Fórmulas dinâmicas (ex: expressão que busca dano na arma do personagem)
        if (params.hasNonNull("expressao_formula")) {
            // Construção do InterpretadorContexto aqui
            InterpretadorContexto intCtx = new InterpretadorContexto(
                (ProcedimentoContexto) ctx, 
                instanciaResolver, 
                cenaRepo, 
                mapper
            );
            
            // Avalia e retorna o resultado da expressão
            return interpretador.interpretar(params.get("expressao_formula"), intCtx).comoTexto();
        }

        // 3. Fallback (Configurações do procedimento)
        String chaveConfgs = params.path("chave_confgs").asString(null);
        if (chaveConfgs != null) {
            String variacaoDados = ctx.getContexto().getStringOrThrow("start_type");
            Procedimento procedimento = ctx.getProcedimento();
            JsonNode confgsGeral = procedimento.getConfigsGeral();
            
            JsonNode tipoNode = confgsGeral.path("start_types").path(variacaoDados);
            if (tipoNode.isMissingNode())
                throw new IllegalArgumentException("Tipo de início desconhecido: " + variacaoDados);

            String formula = tipoNode.path(chaveConfgs).asString();
            if (formula == null || formula.isBlank())
                throw new IllegalArgumentException("Fórmula '" + chaveConfgs + "' não encontrada para " + variacaoDados);
            
            return formula;
        }

        throw new IllegalArgumentException("Nenhuma fórmula, expressão ou chave_confgs fornecida na etapa.");
    }

    private Map<String, Object> parseFormula(String formula) {
        // (Seu método parseFormula permanece inalterado)
        String limpa = formula.replaceAll("\\s+", "");
        Map<String, Object> resultado = new LinkedHashMap<>();

        Pattern somaPattern = Pattern.compile("^(d\\d+)\\+(\\d+|d\\d+)$");
        Matcher somaMatcher = somaPattern.matcher(limpa);
        if (somaMatcher.matches()) {
            String dado1 = somaMatcher.group(1);
            String termo2 = somaMatcher.group(2);
            List<String> dados = new ArrayList<>();
            dados.add(dado1);
            if (termo2.startsWith("d")) {
                dados.add(termo2);
                resultado.put("dados", dados);
            } else {
                resultado.put("dados", dados);
                resultado.put("modificador", Integer.parseInt(termo2));
            }
            return resultado;
        }

        Pattern multPattern = Pattern.compile("^(\\d+)d(\\d+)$");
        Matcher multMatcher = multPattern.matcher(limpa);
        if (multMatcher.matches()) {
            int qtd = Integer.parseInt(multMatcher.group(1));
            String dado = "d" + multMatcher.group(2);
            List<String> dados = new ArrayList<>();
            for (int i = 0; i < qtd; i++) dados.add(dado);
            resultado.put("dados", dados);
            return resultado;
        }

        if (limpa.matches("^d\\d+$")) {
            resultado.put("dados", List.of(limpa));
            return resultado;
        }

        throw new IllegalArgumentException("Fórmula não suportada: " + formula);
    }

    private String exigirTexto(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v.asString();
    }
}