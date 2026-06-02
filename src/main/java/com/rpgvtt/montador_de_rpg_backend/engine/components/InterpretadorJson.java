package com.rpgvtt.montador_de_rpg_backend.engine.components;

import com.rpgvtt.montador_de_rpg_backend.engine.utils.Alvo;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.Contexto;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.ResultadoExpressao;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.*;

@Component
public class InterpretadorJson {

    public ResultadoExpressao interpretar(JsonNode expressao, Contexto contexto) {
        if (expressao == null || !expressao.isObject()) {
            throw new IllegalArgumentException("Expressão deve ser um objeto JSON");
        }
        JsonNode tipoNode = expressao.get("tipo");
        if (tipoNode == null || tipoNode.isNull()) {
            throw new IllegalArgumentException("Expressão sem campo 'tipo'");
        }
        String tipo = tipoNode.asString();
        return switch (tipo) {
            case "constante"         -> resolverConstante(expressao);
            case "caminho"           -> resolverCaminho(expressao, contexto);
            case "caminho_coringa"   -> resolverCaminhoCoringa(expressao, contexto);
            case "formula"           -> resolverFormula(expressao, contexto);
            case "formula_raw"       -> resolverFormulaRaw(expressao);
            case "condicao"          -> resolverCondicao(expressao, contexto);
            case "condicao_composta" -> resolverCondicaoComposta(expressao, contexto);
            case "ternario"          -> resolverTernario(expressao, contexto);
            case "filtro"            -> resolverFiltro(expressao, contexto);
            case "funcao"            -> resolverFuncao(expressao, contexto);
            case "alvo"              -> resolverAlvo(expressao, contexto);
            case "texto"             -> resolverTexto(expressao, contexto);
            case "instrucao"         -> resolverInstrucao(expressao);
            case "objeto"            -> resolverObjeto(expressao);
            default -> throw new IllegalArgumentException("Tipo de expressão desconhecido: '" + tipo + "'");
        };
    }

    private ResultadoExpressao resolverConstante(JsonNode expr) {
        JsonNode valorNode = expr.get("valor");
        if (valorNode == null) return ResultadoExpressao.nulo();

        if (valorNode.isNumber()) {
            return ResultadoExpressao.numero(valorNode.asDouble());
        }
        if (valorNode.isString()) {
            return ResultadoExpressao.texto(valorNode.asString());
        }
        if (valorNode.isBoolean()) {
            return ResultadoExpressao.booleano(valorNode.asBoolean());
        }
        return ResultadoExpressao.nulo();
    }

    private ResultadoExpressao resolverCaminho(JsonNode expr, Contexto ctx) {
        JsonNode caminhoNode = expr.get("caminho");
        if (caminhoNode == null || !caminhoNode.isString()) {
            throw new IllegalArgumentException("'caminho' é obrigatório para tipo 'caminho'");
        }
        String caminho = caminhoNode.asString();
        Optional<Object> optValor = ctx.get(caminho);
        if (optValor.isEmpty()) {
            if (expr.has("padrao")) {
                return interpretar(expr.get("padrao"), ctx);
            }
            throw new IllegalArgumentException("Caminho não encontrado: " + caminho);
        }
        return converterParaResultado(optValor.get());
    }

    private ResultadoExpressao resolverCaminhoCoringa(JsonNode expr, Contexto ctx) {
        JsonNode baseNode = expr.get("base");
        JsonNode propNode = expr.get("propriedade");
        if (baseNode == null || !baseNode.isString() || propNode == null || !propNode.isString()) {
            throw new IllegalArgumentException("'base' e 'propriedade' são obrigatórios em caminho_coringa");
        }
        Optional<Object> optBase = ctx.get(baseNode.asString());
        if (optBase.isEmpty()) {
            return ResultadoExpressao.lista(Collections.emptyList());
        }
        if (!(optBase.get() instanceof List<?> lista)) {
            throw new IllegalArgumentException("Caminho base '" + baseNode.asString() + "' não é uma lista");
        }
        List<Object> resultados = new ArrayList<>();
        for (Object item : lista) {
            if (item instanceof Map<?, ?> map) {
                resultados.add(map.get(propNode.asString()));
            } else {
                throw new UnsupportedOperationException(
                    "Item da lista não é um Map; não é possível extrair propriedade '" + propNode.asString() + "'");
            }
        }
        return ResultadoExpressao.lista(resultados);
    }

    private ResultadoExpressao resolverFormula(JsonNode expr, Contexto ctx) {
        JsonNode aNode = expr.get("operandoA");
        JsonNode bNode = expr.get("operandoB");
        JsonNode opNode = expr.get("operador");
        if (aNode == null || bNode == null || opNode == null || !opNode.isString()) {
            throw new IllegalArgumentException("'operandoA', 'operandoB' e 'operador' são obrigatórios na fórmula");
        }
        double a = interpretar(aNode, ctx).comoNumero();
        double b = interpretar(bNode, ctx).comoNumero();
        String operador = opNode.asString();
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
        if (exprNode == null || !exprNode.isString()) {
            throw new IllegalArgumentException("'expressao' é obrigatório em formula_raw");
        }
        double resultado = FormulaSolver.execute(exprNode.asString());
        return ResultadoExpressao.numero(resultado);
    }

    private ResultadoExpressao resolverCondicao(JsonNode expr, Contexto ctx) {
        JsonNode aNode = expr.get("operandoA");
        JsonNode bNode = expr.get("operandoB");
        JsonNode opNode = expr.get("operador");
        if (aNode == null || bNode == null || opNode == null || !opNode.isString()) {
            throw new IllegalArgumentException("'operandoA', 'operandoB' e 'operador' são obrigatórios na condição");
        }
        ResultadoExpressao resA = interpretar(aNode, ctx);
        ResultadoExpressao resB = interpretar(bNode, ctx);
        String operador = opNode.asString();

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
        String operador = opNode.asString();
        List<JsonNode> condicoes = new ArrayList<>();
        for (JsonNode n : condsNode) condicoes.add(n);

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
            Contexto itemCtx = criarContextoItem(item);
            ResultadoExpressao condRes = interpretar(condNode, itemCtx);
            if (condRes.comoBooleano()) {
                resultado.add(item);
            }
        }
        return ResultadoExpressao.lista(resultado);
    }

    private Contexto criarContextoItem(Object item) {
        return new Contexto() {
            @Override
            public Optional<Object> get(String caminho) {
                if (!caminho.startsWith("item")) {
                    return Optional.empty();
                }
                if (caminho.equals("item")) {
                    return Optional.ofNullable(item);
                }
                String resto = caminho.substring(5); // remove "item."
                if (resto.isEmpty()) return Optional.ofNullable(item);
                if (item instanceof Map<?, ?> map) {
                    return Optional.ofNullable(resolverCaminhoEmMap(map, resto));
                }
                return Optional.empty();
            }
        };
    }

    private Object resolverCaminhoEmMap(Map<?, ?> map, String caminho) {
        String[] partes = caminho.split("\\.");
        Object atual = map;
        for (String parte : partes) {
            if (atual instanceof Map<?, ?> m) {
                atual = m.get(parte);
            } else {
                return null;
            }
        }
        return atual;
    }

    private ResultadoExpressao resolverFuncao(JsonNode expr, Contexto ctx) {
        JsonNode nomeNode = expr.get("nome");
        JsonNode argsNode = expr.get("argumentos");
        if (nomeNode == null || !nomeNode.isString() || argsNode == null || !argsNode.isArray()) {
            throw new IllegalArgumentException("'nome' (texto) e 'argumentos' (array) são obrigatórios na função");
        }
        String nome = nomeNode.asString();
        List<ResultadoExpressao> args = new ArrayList<>();
        for (JsonNode arg : argsNode) {
            args.add(interpretar(arg, ctx));
        }

        // Validação de aridade
        switch (nome) {
            case "ceil", "floor", "round", "abs", "size" -> {
                if (args.size() < 1) throw new IllegalArgumentException(nome + " requer pelo menos 1 argumento");
            }
            case "min", "max" -> {
                if (args.size() != 2) throw new IllegalArgumentException(nome + " requer exatamente 2 argumentos");
            }
            case "clamp" -> {
                if (args.size() != 3) throw new IllegalArgumentException("clamp requer 3 argumentos");
            }
            case "if" -> {
                if (args.size() != 3) throw new IllegalArgumentException("if requer 3 argumentos");
            }
            case "sumList", "averageList" -> {
                if (args.size() != 2) throw new IllegalArgumentException(nome + " requer 2 argumentos (lista, expressão de mapeamento)");
            }
            case "contains" -> {
                if (args.size() != 2) throw new IllegalArgumentException("contains requer 2 argumentos (lista, valor)");
            }
            case "concat" -> {
                if (args.size() < 1) throw new IllegalArgumentException("concat requer pelo menos 1 argumento");
            }
            case "ifNull" -> {
                if (args.size() != 2) throw new IllegalArgumentException("ifNull requer 2 argumentos (valor, padrao)");
            }
        }

        return switch (nome) {
            case "ceil" -> ResultadoExpressao.numero(Math.ceil(args.get(0).comoNumero()));
            case "floor" -> ResultadoExpressao.numero(Math.floor(args.get(0).comoNumero()));
            case "round" -> ResultadoExpressao.numero(Math.round(args.get(0).comoNumero()));
            case "abs" -> ResultadoExpressao.numero(Math.abs(args.get(0).comoNumero()));
            case "min" -> ResultadoExpressao.numero(Math.min(args.get(0).comoNumero(), args.get(1).comoNumero()));
            case "max" -> ResultadoExpressao.numero(Math.max(args.get(0).comoNumero(), args.get(1).comoNumero()));
            case "clamp" -> {
                double v = args.get(0).comoNumero();
                double mn = args.get(1).comoNumero();
                double mx = args.get(2).comoNumero();
                yield ResultadoExpressao.numero(Math.clamp(v, mn, mx));
            }
            case "size" -> {
                Object arg = args.get(0).getValor();
                if (arg instanceof List<?> lista) yield ResultadoExpressao.numero(lista.size());
                throw new IllegalArgumentException("size requer uma lista");
            }
            case "sumList" -> {
                List<?> lista = args.get(0).comoLista();
                JsonNode mapExpr = argsNode.get(1);
                double soma = 0;
                for (Object item : lista) {
                    Contexto itemCtx = criarContextoItem(item);
                    soma += interpretar(mapExpr, itemCtx).comoNumero();
                }
                yield ResultadoExpressao.numero(soma);
            }
            case "averageList" -> {
                List<?> lista = args.get(0).comoLista();
                if (lista.isEmpty()) yield ResultadoExpressao.numero(0);
                JsonNode mapExpr = argsNode.get(1);
                double soma = 0;
                for (Object item : lista) {
                    Contexto itemCtx = criarContextoItem(item);
                    soma += interpretar(mapExpr, itemCtx).comoNumero();
                }
                yield ResultadoExpressao.numero(soma / lista.size());
            }
            case "if" -> {
                boolean cond = args.get(0).comoBooleano();
                yield cond ? args.get(1) : args.get(2);
            }
            case "contains" -> {
                List<?> lista = args.get(0).comoLista();
                yield ResultadoExpressao.booleano(lista.contains(args.get(1).getValor()));
            }
            case "concat" -> {
                StringBuilder sb = new StringBuilder();
                for (ResultadoExpressao a : args) sb.append(a.getValor().toString());
                yield ResultadoExpressao.texto(sb.toString());
            }
            case "ifNull" -> {
                if (args.get(0).getTipo() == ResultadoExpressao.TipoResultado.NULO) {
                    yield args.get(1);
                } else {
                    yield args.get(0);
                }
            }
            default -> throw new IllegalArgumentException("Função desconhecida: " + nome);
        };
    }

    private ResultadoExpressao resolverAlvo(JsonNode expr, Contexto ctx) {
        JsonNode tipoEntNode = expr.get("tipoEntidade");
        JsonNode idNode = expr.get("id");
        if (tipoEntNode == null || !tipoEntNode.isString() || idNode == null) {
            throw new IllegalArgumentException("'tipoEntidade' (texto) e 'id' são obrigatórios no alvo");
        }
        String tipoEntidade = tipoEntNode.asString();
        Object id;
        if (idNode.isObject() && idNode.has("tipo")) {
            ResultadoExpressao idRes = interpretar(idNode, ctx);
            id = idRes.getValor();
        } else if (idNode.isNumber()) {
            id = idNode.numberValue();
        } else if (idNode.isString()) {
            id = idNode.asString();
        } else {
            throw new IllegalArgumentException("'id' deve ser número, texto ou uma expressão");
        }
        return ResultadoExpressao.alvo(new Alvo(tipoEntidade, id));
    }

    private ResultadoExpressao resolverTexto(JsonNode expr, Contexto ctx) {
        JsonNode templateNode = expr.get("template");
        if (templateNode == null || !templateNode.isString()) {
            throw new IllegalArgumentException("'template' é obrigatório em texto");
        }
        String template = templateNode.asString();
        JsonNode varsNode = expr.get("variaveis");
        if (varsNode != null && varsNode.isObject()) {
            // Itera sobre as propriedades do objeto "variaveis"
            for (Map.Entry<String, JsonNode> entry : varsNode.properties()) {
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

    private ResultadoExpressao resolverObjeto(JsonNode expr) {
        JsonNode valorNode = expr.get("valor");
        if (valorNode == null) {
            return ResultadoExpressao.nulo();
        }
        Object obj = converterParaObjetoJava(valorNode);
        return ResultadoExpressao.objeto(obj);
    }

    private ResultadoExpressao converterParaResultado(Object valor) {
        if (valor == null) return ResultadoExpressao.nulo();
        if (valor instanceof Number n) return ResultadoExpressao.numero(n.doubleValue());
        if (valor instanceof String s) return ResultadoExpressao.texto(s);
        if (valor instanceof Boolean b) return ResultadoExpressao.booleano(b);
        if (valor instanceof List<?> l) return ResultadoExpressao.lista(l);
        return ResultadoExpressao.texto(valor.toString());
    }

    private Object converterParaObjetoJava(JsonNode node) {
        if (node.isNull()) return null;
        if (node.isNumber()) return node.numberValue();
        if (node.isString()) return node.asString();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isArray()) {
            List<Object> lista = new ArrayList<>();
            for (JsonNode item : node) {
                lista.add(converterParaObjetoJava(item));
            }
            return lista;
        }
        if (node.isObject()) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                mapa.put(entry.getKey(), converterParaObjetoJava(entry.getValue()));
            }
            return mapa;
        }
        return null;
    }
}