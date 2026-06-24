package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.engine.components.InterpretadorJson;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InterpretadorContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador.ResultadoExpressao;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class VerificarCondicaoHandler implements EtapaHandler {

    private final InstanciaResolver instanciaResolver;
    private final InterpretadorJson interpretador;
    private final CenaRepository cenaRepo;
    private final JsonMapper mapper;


    @Override
    public String tipoEtapa() { return "VERIFICAR_CONDICAO"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode condicaoNode = etapa.getParametrosEtapa().get("condicao");
        return verificar(condicaoNode, ctx);
    }

    public ResultadoEtapa verificar(JsonNode condicaoNode, ExecucaoContexto ctx) {
        if (condicaoNode == null || condicaoNode.isMissingNode()) {
            return ResultadoEtapa.concluida(Map.of("status", "sem_condicao"));
        }

        // Se for uma expressão do InterpretadorJson (objeto com "tipo"), delegue
        if (condicaoNode.isObject() && condicaoNode.has("tipo")) {
            InterpretadorContexto intCtx = new InterpretadorContexto(
                (ProcedimentoContexto) ctx, instanciaResolver, cenaRepo, mapper);
            ResultadoExpressao resultado = interpretador.interpretar(condicaoNode, intCtx);
            boolean ok = resultado.comoBooleano();
            if (!ok) {
                return ResultadoEtapa.concluida(Map.of("status", "condicao_falhou"));
            }
            return ResultadoEtapa.concluida(Map.of("status", "condicao_ok"));
        }

        return verificarLegado(condicaoNode, ctx);
    }

    private ResultadoEtapa verificarLegado(JsonNode condicao, ExecucaoContexto ctx) {
        if (condicao == null || condicao.isMissingNode()) {
            return ResultadoEtapa.concluida(Map.of("status", "sem_condicao"));
        }

        for (Map.Entry<String, JsonNode> entry : condicao.properties()) {
            String campo = entry.getKey();
            JsonNode valorEsperado = entry.getValue();

            Object valorReal = resolverValor(campo, ctx);
            if (valorReal == null) {
                return ResultadoEtapa.erro("Campo '" + campo + "' não encontrado");
            }

            boolean ok;
            if (valorEsperado.isBoolean()) {
                ok = (valorReal instanceof Boolean b && b == valorEsperado.asBoolean());
            } else if (valorEsperado.isNumber()) {
                double esperado = valorEsperado.asDouble();
                ok = (valorReal instanceof Number num) && num.doubleValue() == esperado;
            } else if (valorEsperado.isObject()) {
                ok = avaliarObjetoCondicao(valorEsperado, valorReal);
            } else {
                ok = valorEsperado.asString().equals(valorReal.toString());
            }

            if (!ok) {
                return ResultadoEtapa.concluida(Map.of(
                        "status", "condicao_falhou",
                        "campo", campo,
                        "esperado", valorEsperado.asString(),
                        "real", valorReal.toString()
                ));
            }
        }

        return ResultadoEtapa.concluida(Map.of("status", "condicao_ok"));
    }

    private Object resolverValor(String campo, ExecucaoContexto ctx) {
        // Tenta obter do contexto
        if (ctx.getContexto().containsKey(campo)) {
            return ctx.getContexto().get(campo, Object.class).orElse(null);
        }
        // Tenta obter da instância ativa
        if (!ctx.semInstancias()) {
            try {
                EntidadeInstancia instancia = instanciaResolver.retornarAtiva(ctx);
                JsonNode atributos = instancia.getAtributosAtuais();
                if (atributos.has(campo)) {
                    JsonNode valor = atributos.get(campo);
                    // Converte para tipo Java apropriado
                    if (valor.isBoolean()) return valor.asBoolean();
                    if (valor.isNumber()) return valor.asDouble();
                    return valor.asString();
                }
            } catch (Exception e) {
                // Instância não encontrada, retorna null
            }
        }
        return null;
    }

    private boolean avaliarObjetoCondicao(JsonNode obj, Object valorReal) {
        if (valorReal instanceof Number num) {
            if (obj.has("min") && num.doubleValue() < obj.get("min").asDouble()) return false;
            if (obj.has("max") && num.doubleValue() > obj.get("max").asDouble()) return false;
            return true;
        }
        if (obj.has("contains") && valorReal instanceof String s) {
            return s.contains(obj.get("contains").asString());
        }
        return false;
    }
}