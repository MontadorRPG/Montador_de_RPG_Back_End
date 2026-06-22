package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.VerificadorAtributo;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlterarAtributoHandler implements EtapaHandler {

    private final InstanciaResolver instanciaResolver;
    private final EntidadeInstanciaRepository instanciaRepo;
    private final VerificadorAtributo verificador;

    @Override
    public String tipoEtapa() { return "ALTERAR_ATRIBUTO"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();

        String atributo = exigirTexto(params, "atributo", etapa);
        String op       = params.path("operacao").asString(""); // soma|sub|mult|div|set|""

        double qtd = resolverQuantidade(params, ctx, etapa);
        EntidadeInstancia inst = resolverInstancia(params, ctx);

        JsonNode atualNode = inst.getAtributosAtuais().get(atributo);
        if (atualNode == null) {
            throw new JsonFieldNotFoundException(atributo, "atributosAtuais da instância " + inst.getId());
        }
        double valorAtual = atualNode.asDouble();

        double resultadoBruto = switch (op) {
            case "", "set" -> qtd;
            case "soma"    -> valorAtual + qtd;
            case "sub"     -> valorAtual - qtd;
            case "mult"    -> valorAtual * qtd;
            case "div" -> {
                if (qtd == 0) throw new IllegalArgumentException(
                        "Divisão por zero em ALTERAR_ATRIBUTO (etapa: " + etapa.getNome() + ")");
                yield valorAtual / qtd;
            }
            default -> throw new IllegalArgumentException(
                    "Operação desconhecida em ALTERAR_ATRIBUTO: '" + op + "'");
        };

        // Mantém o tipo numérico original (a maioria dos atributos é inteira);
        // só div pode legitimamente produzir fração.
        boolean manterInteiro = !"div".equals(op);

        if (!verificador.verificarAtributo(ctx.getIdSistema(), atributo, resultadoBruto)) {
            return ResultadoEtapa.erro(
                    "Valor inválido para '" + atributo + "': " + resultadoBruto +
                            " (etapa: " + etapa.getNome() + ")");
        }

        ObjectNode attrs = (ObjectNode) inst.getAtributosAtuais();
        if (manterInteiro) {
            attrs.put(atributo, (long) resultadoBruto);
        } else {
            attrs.put(atributo, resultadoBruto);
        }
        inst.setAtributosAtuais(attrs);
        instanciaRepo.save(inst);

        return ResultadoEtapa.concluida(Map.of(
                "idInstancia", inst.getId(),
                "atributo",    atributo,
                "valorAnterior", valorAtual,
                "valorNovo",     manterInteiro ? (long) resultadoBruto : resultadoBruto
        ));
    }

    private double resolverQuantidade(JsonNode params, ExecucaoContexto ctx, EtapaExecutavel etapa) {
        if (params.has("source_key")) {
            String sourceKey = params.get("source_key").asString();
    
            // Lê como Object — aceita Integer, Double, String, Long etc.
            Object raw = ctx.getContexto().get(sourceKey, Object.class).orElse(null);
            if (raw == null) {
                throw new JsonFieldNotFoundException(sourceKey, etapa.getNome());
            }
            try {
                return Double.parseDouble(raw.toString().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Valor '" + raw + "' em '" + sourceKey + "' não é numérico (etapa: " + etapa.getNome() + ")"
                );
            }
        }
        if (params.has("quantidade")) {
            return params.get("quantidade").asDouble();
        }
        throw new JsonFieldNotFoundException("quantidade ou source_key", etapa.getNome());
    }

    private EntidadeInstancia resolverInstancia(JsonNode params, ExecucaoContexto ctx) {
        JsonNode idNode = params.path("id_entidade");
        if (idNode.isMissingNode() || idNode.isNull()) {
            return instanciaResolver.retornarAtiva(ctx);
        }
        Long idEntidade = idNode.asLong();
        return instanciaRepo.findById(idEntidade)
                .orElseThrow(() -> new EntityNotFoundException(EntidadeInstancia.class, idEntidade));
    }

    private String exigirTexto(JsonNode params, String chave, EtapaExecutavel etapa) {
        JsonNode v = params.path(chave);
        if (v.isMissingNode() || v.isNull()) throw new JsonFieldNotFoundException(chave, etapa.getNome());
        return v.asString();
    }
}




