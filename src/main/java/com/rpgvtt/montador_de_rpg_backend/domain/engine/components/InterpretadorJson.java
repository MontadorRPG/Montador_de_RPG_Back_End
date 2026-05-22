package com.rpgvtt.montador_de_rpg_backend.domain.engine.components;

import com.rpgvtt.montador_de_rpg_backend.domain.engine.Contexto;
import com.rpgvtt.montador_de_rpg_backend.domain.engine.utils.ResultadoExpressao;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.*;

import java.util.*;

@Component
public class InterpretadorJson {

    private final ObjectMapper objectMapper;

    public InterpretadorJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ResultadoExpressao interpretar(JsonNode expressao, Contexto contexto) {
        if (expressao == null || !expressao.isObject()) {
            throw new IllegalArgumentException("Expressão deve ser um objeto JSON");
        }
        JsonNode tipoNode = expressao.get("tipo");
        if (tipoNode == null || tipoNode.isNull()) {
            throw new IllegalArgumentException("Expressão sem campo 'tipo'");
        }
        String tipo = tipoNode.asText();
        return switch (tipo) {
            case "constante"         -> resolverConstante(expressao);
            case "caminho"           -> resolverCaminho(expressao, contexto);
            case "formula"           -> resolverFormula(expressao, contexto);
            case "formula_raw"       -> resolverFormulaRaw(expressao);
            case "condicao"          -> resolverCondicao(expressao, contexto);
            case "condicao_composta" -> resolverCondicaoComposta(expressao, contexto);
            case "ternario"          -> resolverTernario(expressao, contexto);
            case "filtro"            -> resolverFiltro(expressao, contexto);
            case "funcao"            -> resolverFuncao(expressao, contexto);
            case "alvo"              -> resolverAlvo(expressao);
            case "texto"             -> resolverTexto(expressao, contexto);
            case "instrucao"         -> resolverInstrucao(expressao);
            default -> throw new IllegalArgumentException("Tipo de expressão desconhecido: '" + tipo + "'");
        };
    }

    private ResultadoExpressao resolverConstante(JsonNode expr) {
        JsonNode valorNode = expr.get("valor");
        if (valorNode == null) return ResultadoExpressao.nulo();

        if (valorNode.isNumber()) {

            return ResultadoExpressao.numero(valorNode.asDouble());
        }
        if (valorNode.isTextual()) {
            return ResultadoExpressao.texto(valorNode.asText());
        }
        if (valorNode.isBoolean()) {
            return ResultadoExpressao.booleano(valorNode.asBoolean());
        }
        return ResultadoExpressao.nulo();
    }

    private ResultadoExpressao resolverCaminho(JsonNode expr, Contexto ctx) {
        JsonNode caminhoNode = expr.get("caminho");
        if (caminhoNode == null || !caminhoNode.isTextual()) {
            throw new IllegalArgumentException("'caminho' é obrigatório para tipo 'caminho'");
        }
        String caminho = caminhoNode.asText();
        Optional<Object> optValor = ctx.get(caminho);
        if (optValor.isEmpty()) {
            // Suporte a valor padrão
            if (expr.has("padrao")) {
                return resolverConstante(expr.get("padrao"));
            }
            throw new IllegalArgumentException("Caminho não encontrado: " + caminho);
        }
        return converterParaResultado(optValor.get());
    }

    private ResultadoExpressao resolverFormula(JsonNode expr, Contexto ctx) {
        JsonNode aNode = expr.get("operandoA");
        JsonNode bNode = expr.get("operandoB");
        JsonNode opNode = expr.get("operador");
        if (aNode == null || bNode == null || opNode == null || !opNode.isTextual()) {
            throw new IllegalArgumentException("'operandoA', 'operandoB' e 'operador' são obrigatórios na fórmula");
        }
        double a = interpretar(aNode, ctx).comoNumero();
        double b = interpretar(bNode, ctx).comoNumero();
        String operador = opNode.asText();
        double resultado = switch (operador) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;
            case "%" -> a % b;
            case "^" -> Math.pow(a, b);
            default -> throw new IllegalArgumentException("Operador inválido: " + operador);
        };
        return ResultadoExpressao.numero(resultado);
    }

    private ResultadoExpressao resolverFormulaRaw(JsonNode expr) {
        JsonNode exprNode = expr.get("expressao");
        if (exprNode == null || !exprNode.isTextual()) {
            throw new IllegalArgumentException("'expressao' é obrigatório em formula_raw");
        }
        double resultado = FormulaSolver.execute(exprNode.asText());
        return ResultadoExpressao.numero(resultado);
    }

    private ResultadoExpressao resolverCondicao(JsonNode expr, Contexto ctx) {
        JsonNode aNode = expr.get("operandoA");
        JsonNode bNode = expr.get("operandoB");
        JsonNode opNode = expr.get("operador");
        if (aNode == null || bNode == null || opNode == null || !opNode.isTextual()) {
            throw new IllegalArgumentException("'operandoA', 'operandoB' e 'operador' são obrigatórios na condição");
        }
        ResultadoExpressao resA = interpretar(aNode, ctx);
        ResultadoExpressao resB = interpretar(bNode, ctx);
        String operador = opNode.asText();

        if (resA.getTipo() == ResultadoExpressao.TipoResultado.NUMERO &&
            resB.getTipo() == ResultadoExpressao.TipoResultado.NUMERO) {
            double a = resA.comoNumero();
            double b = resB.comoNumero();
            boolean r = switch (operador) {
                case "<" -> a < b;
                case ">" -> a > b;
                case "<=" -> a <= b;
                case ">=" -> a >= b;
                case "==" -> a == b;
                case "!=" -> a != b;
                default -> throw new IllegalArgumentException("Operador numérico inválido: " + operador);
            };
            return ResultadoExpressao.booleano(r);
        }

        if (resA.getTipo() == ResultadoExpressao.TipoResultado.TEXTO &&
            resB.getTipo() == ResultadoExpressao.TipoResultado.TEXTO) {
            String a = resA.comoTexto();
            String b = resB.comoTexto();
            boolean r = switch (operador) {
                case "==" -> a.equals(b);
                case "!=" -> !a.equals(b);
                default -> throw new IllegalArgumentException("Texto só suporta == e !=, recebido: " + operador);
            };
            return ResultadoExpressao.booleano(r);
        }

        throw new IllegalArgumentException("Tipos incompatíveis para condição: " + resA.getTipo() + " e " + resB.getTipo());
    }

    private ResultadoExpressao resolverCondicaoComposta(JsonNode expr, Contexto ctx) {
        JsonNode opNode = expr.get("operador");
        JsonNode condsNode = expr.get("condicoes");
        if (opNode == null || condsNode == null || !condsNode.isArray() || condsNode.size() == 0) {
            throw new IllegalArgumentException("'operador' e 'condicoes' (array não vazio) são obrigatórios");
        }
        String operador = opNode.asText();
        List<JsonNode> condicoes = new ArrayList<>();
        condsNode.forEach(condicoes::add);

        boolean resultado;
        if ("E".equals(operador)) {
            resultado = condicoes.stream().allMatch(c -> interpretar(c, ctx).comoBooleano());
        } else if ("OU".equals(operador)) {
            resultado = condicoes.stream().anyMatch(c -> interpretar(c, ctx).comoBooleano());
        } else {
            throw new IllegalArgumentException("Operador composto deve ser 'E' ou 'OU'");
        }
        return ResultadoExpressao.booleano(resultado);
    }

    private ResultadoExpressao resolverTernario(JsonNode expr, Contexto ctx) {
        JsonNode condNode = expr.get("condicao");
        JsonNode thenNode = expr.get("entao");
        JsonNode elseNode = expr.get("senao");
        if (condNode == null || thenNode == null || elseNode == null) {
            throw new IllegalArgumentException("'condicao', 'entao' e 'senao' são obrigatórios no ternário");
        }
        boolean cond = interpretar(condNode, ctx).comoBooleano();
        return interpretar(cond ? thenNode : elseNode, ctx);
    }

    private ResultadoExpressao resolverFiltro(JsonNode expr, Contexto ctx) {
        JsonNode listaNode = expr.get("lista");
        JsonNode condNode = expr.get("condicao");
        if (listaNode == null || condNode == null) {
            throw new IllegalArgumentException("'lista' e 'condicao' são obrigatórios no filtro");
        }
        ResultadoExpressao listaRes = interpretar(listaNode, ctx);

        Object rawLista = listaRes.getValor();
        if (!(rawLista instanceof List<?> lista)) {
            throw new IllegalArgumentException("Filtro requer uma lista, mas veio: " + rawLista);
        }
        List<Object> resultado = new ArrayList<>();
        for (Object item : lista) {
            
            Contexto itemCtx = caminho -> {
                if ("item".equals(caminho)) return Optional.ofNullable(item);
                
                return Optional.empty();
            };
            ResultadoExpressao condRes = interpretar(condNode, itemCtx);
            if (condRes.comoBooleano()) {
                resultado.add(item);
            }
        }
        return ResultadoExpressao.lista(resultado);
    }

    private ResultadoExpressao resolverFuncao(JsonNode expr, Contexto ctx) {
        JsonNode nomeNode = expr.get("nome");
        JsonNode argsNode = expr.get("argumentos");
        if (nomeNode == null || !nomeNode.isTextual() || argsNode == null || !argsNode.isArray()) {
            throw new IllegalArgumentException("'nome' (texto) e 'argumentos' (array) são obrigatórios na função");
        }
        String nome = nomeNode.asText();
        List<ResultadoExpressao> args = new ArrayList<>();
        for (JsonNode arg : argsNode) {
            args.add(interpretar(arg, ctx));
        }
        return switch (nome) {

            // Aredondar p/ baixo
            case "ceil" -> {
                double val = args.get(0).comoNumero();
                yield ResultadoExpressao.numero(Math.ceil(val));
            }

            // Aredondar p/ cima
            case "floor" -> {
                double val = args.get(0).comoNumero();
                yield ResultadoExpressao.numero(Math.floor(val));
            }

            // Aredondar p/ o mais proximo
            case "round" -> {
                double val = args.get(0).comoNumero();
                yield ResultadoExpressao.numero(Math.round(val));
            }

            // Valor absoluto
            case "abs" -> {
                double val = args.get(0).comoNumero();
                yield ResultadoExpressao.numero(Math.abs(val));
            }
            
            // Min / Max

            case "min" -> {
                double a = args.get(0).comoNumero();
                double b = args.get(1).comoNumero();
                yield ResultadoExpressao.numero(Math.min(a, b));
            }

            case "max" -> {
                double a = args.get(0).comoNumero();
                double b = args.get(1).comoNumero();
                yield ResultadoExpressao.numero(Math.max(a, b));
            }
            
            // Limita um valor entre um minimo e um maximo
            case "clamp" -> {
                double valor = args.get(0).comoNumero();
                double min = args.get(1).comoNumero();
                double max = args.get(2).comoNumero();
                double clamped = Math.min(Math.max(valor, min), max);
                yield ResultadoExpressao.numero(clamped);
            }

            // Tamanho de uma lista
            case "size" -> {
                if (args.size() != 1) throw new IllegalArgumentException("tamanho requer 1 argumento");
                Object arg = args.get(0).getValor();
                if (arg instanceof List<?> lista) {
                    yield ResultadoExpressao.numero(lista.size());
                }
                throw new IllegalArgumentException("tamanho requer uma lista");
            }

            // Soma elementos de uma lista
            case "sumList" -> {
                if (args.size() != 1) throw new IllegalArgumentException("soma_lista requer 1 argumento");
                List<?> lista = args.get(0).comoLista();
                double soma = 0;
                for (Object item : lista) {
                    if (item instanceof Number n) soma += n.doubleValue();
                    else throw new IllegalArgumentException("Item da lista não é número: " + item);
                }
                yield ResultadoExpressao.numero(soma);
            }

            // Media dos valores de uma lista
            case "averageList" -> {
                List<?> lista = args.get(0).comoLista();
                if (lista.isEmpty()) yield ResultadoExpressao.numero(0);
                double soma = 0;
                for (Object item : lista) {
                    soma += ((Number) item).doubleValue();
                }
                yield ResultadoExpressao.numero(soma / lista.size());
            }

            // Talvez seja redundante, mas se precisar ta aí
            case "if" -> {
                if (args.size() != 3) {
                    throw new IllegalArgumentException("if requer 3 argumentos: condição, valorVerdadeiro, valorFalso");
                }
                    boolean cond = args.get(0).comoBooleano();
                    yield cond ? args.get(1) : args.get(2);
            }
                
            default -> throw new IllegalArgumentException("Função desconhecida: " + nome);
        };
    }

    private ResultadoExpressao resolverAlvo(JsonNode expr) {
        JsonNode tipoEntNode = expr.get("tipoEntidade");
        JsonNode idNode = expr.get("id");

        if (tipoEntNode == null || !tipoEntNode.isTextual() || idNode == null) {
            throw new IllegalArgumentException(
                "'tipoEntidade' (texto) e 'id' são obrigatórios no alvo"
            );
        }
        String tipoEntidade = tipoEntNode.asText();
        Object id;

        if (idNode.isNumber()) {
            id = idNode.numberValue();   // Long, Integer, Double
        } else if (idNode.isTextual()) {
            id = idNode.asText();
        } else {
            // Se for uma expressão, teria que avaliá-la – aqui simplificamos
            throw new IllegalArgumentException("'id' deve ser número ou texto");
        }

        Alvo alvo = new Alvo(tipoEntidade, id);
        return ResultadoExpressao.alvo(alvo);
    }

    private ResultadoExpressao resolverTexto(JsonNode expr, Contexto ctx) {
        JsonNode templateNode = expr.get("template");
        if (templateNode == null || !templateNode.isTextual()) {
            throw new IllegalArgumentException("'template' é obrigatório em texto");
        }
        String template = templateNode.asText();
        JsonNode varsNode = expr.get("variaveis");
        if (varsNode != null && varsNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = varsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String nomeVar = entry.getKey();
                JsonNode exprVar = entry.getValue();
                String valor = interpretar(exprVar, ctx).getValor().toString();
                template = template.replace("{" + nomeVar + "}", valor);
            }
        }
        return ResultadoExpressao.texto(template);
    }

    private ResultadoExpressao resolverInstrucao(JsonNode expr) {
        
        return ResultadoExpressao.instrucao(expr);
    }

    private ResultadoExpressao converterParaResultado(Object valor) {
        if (valor == null) return ResultadoExpressao.nulo();
        if (valor instanceof Number n) return ResultadoExpressao.numero(n.doubleValue());
        if (valor instanceof String s) return ResultadoExpressao.texto(s);
        if (valor instanceof Boolean b) return ResultadoExpressao.booleano(b);
        if (valor instanceof List<?> l) return ResultadoExpressao.lista(l);
        return ResultadoExpressao.texto(valor.toString());
    }
}