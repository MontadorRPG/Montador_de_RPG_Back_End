package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.HandlerRegistry;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.*;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeSistemaRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
// @RequiredArgsConstructor
public class CustomizarEntidadeHandler implements EtapaHandler {

    private final EntidadeSistemaRepository entidadeSistemaRepo;
    private final InstanciaResolver instanciaResolver;
    private final EntidadeInstanciaRepository instanciaRepo;
    private final ObjectMapper mapper;

    @Lazy
    @Autowired
    private final HandlerRegistry handlers;

    public CustomizarEntidadeHandler(
            EntidadeSistemaRepository entidadeSistemaRepo,
            InstanciaResolver instanciaResolver,
            EntidadeInstanciaRepository instanciaRepo,
            @Lazy HandlerRegistry handlers, 
            ObjectMapper mapper) {
        this.entidadeSistemaRepo = entidadeSistemaRepo;
        this.instanciaResolver = instanciaResolver;
        this.instanciaRepo = instanciaRepo;
        this.handlers = handlers;
        this.mapper = mapper;
    }

    @Override
    public String tipoEtapa() { return "CUSTOMIZAR_ENTIDADE"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();
        Long idEntidadeSistema = params.get("id_entidade_sistema").asLong();
        String chaveRolagem = params.path("chave_rolagem").asString("customizacao_rolagem");

        EntidadeSistema entidade = entidadeSistemaRepo.findById(idEntidadeSistema)
                .orElseThrow(() -> new IllegalArgumentException("Entidade não encontrada"));

        String chaveTabela = params.path("chave_tabela").asString("tabela_customizacao");
        JsonNode tabelaNode = entidade.getPropriedades().path(chaveTabela);
        if (tabelaNode.isMissingNode()) {
            return ResultadoEtapa.concluida(Map.of("status", "sem_tabela"));
        }

        // Se ainda não rolou, solicita ao front
        if (!ctx.getContexto().containsKey(chaveRolagem)) {
            JsonNode dadosNode = tabelaNode.get("dados");
            List<String> dados = new ArrayList<>();
            for (JsonNode d : dadosNode) dados.add(d.asString());

            return ResultadoEtapa.aguardandoInput(Map.of(
                "tipo_input", "ROLAGEM",
                "campoPedido", "Customizar " + entidade.getNome(),
                "salvar_em", chaveRolagem,
                "rolagem", Map.of("dados", dados)
            ));
        }

        // Lê os resultados (ex: [3, 5])
        JsonNode rolagemNode = ctx.getContexto().get(chaveRolagem, JsonNode.class)
                .orElse(mapper.createArrayNode());
        List<Integer> rolagens = new ArrayList<>();
        for (JsonNode r : rolagemNode) rolagens.add(r.asInt());

        // Encontra a entrada
        JsonNode entradas = tabelaNode.get("entradas");
        JsonNode entrada = null;
        for (JsonNode e : entradas) {
            JsonNode valoresNode = e.get("valores");
            boolean match = true;
            for (int i = 0; i < rolagens.size(); i++) {
                if (valoresNode.get(i).asInt() != rolagens.get(i)) {
                    match = false;
                    break;
                }
            }
            if (match) { entrada = e; break; }
        }

        if (entrada == null) {
            return ResultadoEtapa.erro("Nenhuma entrada para " + rolagens);
        }

        // Executa a ação
        JsonNode acoesNode = entrada.get("acao");
        if (acoesNode != null && acoesNode.isArray()) {
            ResultadoEtapa ultimo = null;
            for (JsonNode acao : acoesNode) {
                ultimo = executarAcao(acao, ctx);
                if (ultimo.tipo() == ResultadoEtapa.Tipo.ERRO) {
                    return ultimo; // para na primeira falha
                }
            }
            return ultimo != null ? ultimo : ResultadoEtapa.concluida(Map.of("status", "customizado"));
        } else {
            JsonNode acao = entrada.get("acao");
            if (acao == null) {
                return ResultadoEtapa.erro("Entrada de tabela sem 'acao' nem 'acoes'");
            }
            return executarAcao(acao, ctx);
        }
    }


    private ResultadoEtapa executarAcao(JsonNode acao, ExecucaoContexto ctx) {
        String tipoAcao = acao.get("tipo").asString();
        return switch (tipoAcao) {
            case "customizacao" -> {
                EntidadeInstancia inst = instanciaResolver.retornarAtiva(ctx);
                ObjectNode custom = inst.getCustomizacoes() instanceof ObjectNode on
                        ? on : mapper.createObjectNode();
                custom.set(acao.get("chave").asString(), acao.get("valor"));
                inst.setCustomizacoes(custom);
                instanciaRepo.save(inst);
                yield ResultadoEtapa.concluida(Map.of("status", "customizado"));
            }
            case "alterar_atributo" -> {
                EtapaProcedimento sub = new EtapaProcedimento();
                sub.setTipoEtapa("ALTERAR_ATRIBUTO");
                sub.setParametrosEtapa(mapper.valueToTree(Map.of(
                    "atributo", acao.get("atributo").asString(),
                    "operacao", acao.get("operacao").asString("set"),
                    "quantidade", acao.get("quantidade").asDouble()
                )));
                yield handlers.get("ALTERAR_ATRIBUTO").executar(sub, ctx);
            }
            case "gerenciar_item" -> {
                EtapaProcedimento sub = new EtapaProcedimento();
                sub.setTipoEtapa("GERENCIAR_ITEM");
                sub.setParametrosEtapa(mapper.valueToTree(Map.of(
                    "tipo", acao.get("modo").asString("CRIAR"),
                    "opcao_fonte_item", "estatico",
                    "opcao_item", acao.get("item_id").asString(),
                    "fonte_qtd", "estatico",
                    "opcao_qtd", "1",
                    "opcao_fonte_personagem", "instancia_ativa"
                )));
                yield handlers.get("GERENCIAR_ITEM").executar(sub, ctx);
            }
            case "chamar_procedimento" -> {
                if (!(ctx instanceof ProcedimentoContexto procedimentoCtx)) {
                    yield ResultadoEtapa.erro("Precisa ser um ProcedimentoContexto");
                }
                EtapaProcedimento sub = new EtapaProcedimento();
                sub.setTipoEtapa("CHAMAR_PROCEDIMENTO");
                sub.setParametrosEtapa(mapper.valueToTree(Map.of(
                    "id_procedimento", acao.get("id_procedimento").asLong(),
                    "salvar_em", "resultado_custom",
                    "escopo", "HERDAR"
                )));
                yield handlers.get("CHAMAR_PROCEDIMENTO").executar(sub, procedimentoCtx);
            }
            default -> ResultadoEtapa.erro("Ação desconhecida: " + tipoAcao);
        };
    }
}