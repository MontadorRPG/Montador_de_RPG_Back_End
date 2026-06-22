package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
// import com.rpgvtt.montador_de_rpg_backend.repository.sistema.SistemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SolicitadorRolagemHandler implements EtapaHandler {

    // private final SistemaRepository sistemaRepository;

    @Override
    public String tipoEtapa() { return "SOLICITAR_ROLAGEM"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();

        String chaveConfgs = exigirTexto(params, "chave_confgs", etapa);
        String campoPedido = params.path("campo_pedido").asString("Role os dados");
        boolean podePassar  = params.path("pode_passar").asBoolean(false);
        String salvarEm     = exigirTexto(params, "salvar_em", etapa);

        String startType = ctx.getContexto().getStringOrThrow("start_type");

        Procedimento procedimento = ctx.getProcedimento();
        JsonNode confgsGeral = procedimento.getConfigsGeral();
        JsonNode startTypes = confgsGeral.path("start_types");

        JsonNode tipoNode = startTypes.path(startType);
        if (tipoNode.isMissingNode())
            throw new IllegalArgumentException("Tipo de início desconhecido: " + startType);

        String formula = tipoNode.path(chaveConfgs).asString();
        if (formula == null || formula.isBlank())
            throw new IllegalArgumentException("Fórmula '" + chaveConfgs + "' não encontrada para " + startType);

        Map<String, Object> rolagemConfig = parseFormula(formula);

        // Se já tiver valor, retorna concluído
        if (ctx.getContexto().containsKey(salvarEm)) {
            Object valor = ctx.getContexto().get(salvarEm, Object.class).orElse(null);
            return ResultadoEtapa.concluida(Map.of(
                "campoPedido", campoPedido,
                "escolha", valor != null ? valor.toString() : "",
                "rolagem", rolagemConfig
            ));
        }

        return ResultadoEtapa.aguardandoInput(Map.of(
            "campoPedido", campoPedido,
            "salvar_em", salvarEm,
            "pode_passar", podePassar,
            "rolagem", rolagemConfig
        ));
    }


    private Map<String, Object> parseFormula(String formula) {
        // Remove espaços
        String limpa = formula.replaceAll("\\s+", "");
        Map<String, Object> resultado = new LinkedHashMap<>();

        // Padrão: "dY+Z" ou "dY+dZ"
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

        // Padrão: "XdY" (ex.: 2d6)
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

        throw new IllegalArgumentException("Fórmula não suportada: " + formula);
    }

    private String exigirTexto(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v.asString();
    }
}