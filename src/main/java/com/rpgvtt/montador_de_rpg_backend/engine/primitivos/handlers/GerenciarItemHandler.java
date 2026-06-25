package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;


import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeRelacao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.components.ItemResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.HandlerRegistry;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaExecutavel;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeRelacaoRepository;
import com.rpgvtt.montador_de_rpg_backend.service.personagem.ItemEfeitoService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GerenciarItemHandler implements EtapaHandler {

    private final JsonMapper mapper;
    private final InstanciaResolver instanciaResolver;
    private final ItemResolver itemResolver;
    private final ItemEfeitoService itemEfeitoService;
    private final EntidadeRelacaoRepository entidadeRelacaoRepo;
    private final EntidadeInstanciaRepository entidadeInstanciaRepo;
    
    private HandlerRegistry handlers;

    public GerenciarItemHandler(JsonMapper mapper,
                                InstanciaResolver instanciaResolver,
                                ItemResolver itemResolver,
                                ItemEfeitoService itemEfeitoService,
                                EntidadeRelacaoRepository entidadeRelacaoRepo,
                                EntidadeInstanciaRepository entidadeInstanciaRepo) {
        this.mapper = mapper;
        this.instanciaResolver = instanciaResolver;
        this.itemResolver = itemResolver;
        this.itemEfeitoService = itemEfeitoService;
        this.entidadeRelacaoRepo = entidadeRelacaoRepo;
        this.entidadeInstanciaRepo = entidadeInstanciaRepo;
    }

    @Lazy
    @Autowired
    public void setHandlers(HandlerRegistry handlers) {
        this.handlers = handlers;
    }

    @Override
    public String tipoEtapa() { return "GERENCIAR_ITEM"; }

    /**
     * parametros_etapa schema:
     * {
     *   "tipo": "CRIAR" | "REMOVER" | "EQUIPAR" | "DESEQUIPAR",
     *   "opcao_fonte_item": "estatico" | "contexto",
     *   "opcao_item": <idInstancia literal>  | <chave do contexto>,
     *   "fonte_qtd": "estatico" | "contexto",       -- only for CRIAR/REMOVER
     *   "opcao_qtd": <int literal> | <chave>,
     *   "opcao_fonte_personagem": "instancia_ativa" | "batalha.aliados" | "batalha.inimigos" | "todos"
     * }
     */
    
    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        Map<String, Object> params = mapper.convertValue(
                etapa.getParametrosEtapa(), new TypeReference<>() {});

        String tipo = (String) params.get("tipo");

        Long idItem = resolverIdItem(params, ctx);
        List<EntidadeInstancia> personagens = resolverPersonagens(params, ctx);

        EntidadeInstancia item = entidadeInstanciaRepo.findById(idItem)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado: " + idItem));

        return switch (tipo) {
            case "CRIAR"       -> inserir(personagens, item, params, ctx);
            case "REMOVER"     -> remover(personagens, item, params);
            case "EQUIPAR"     -> equipar(personagens, item, ctx);
            case "DESEQUIPAR"  -> desequipar(personagens, item, ctx);
            default -> ResultadoEtapa.erro("tipo de GERENCIAR_ITEM desconhecido: " + tipo);
        };
    }

    // ── INSERIR: grant item to character(s), stacking quantity if already held ──

    private ResultadoEtapa inserir(List<EntidadeInstancia> personagens,
                                 EntidadeInstancia item,
                                 Map<String, Object> params,
                                 ExecucaoContexto ctx) {
        int qtd = resolverQtd(params, ctx);
        Map<Long, Integer> resultado = new LinkedHashMap<>();

        for (EntidadeInstancia personagem : personagens) {
            Optional<EntidadeRelacao> existente =
                    itemResolver.buscarRelacao(personagem.getId(), item.getId());

            int novaQtd;
            if (existente.isPresent()) {
                EntidadeRelacao rel = existente.get();
                novaQtd = rel.getQuantidade() + qtd;
                rel.setQuantidade(novaQtd);
                entidadeRelacaoRepo.save(rel);
            } else {
                EntidadeRelacao rel = new EntidadeRelacao();
                rel.setEntidadePai(personagem);
                rel.setEntidadeFilha(item);
                rel.setQuantidade(qtd);
                entidadeRelacaoRepo.save(rel);
                novaQtd = qtd;
            }
            resultado.put(personagem.getId(), novaQtd);
        }

        ResultadoEtapa customizacao = dispararCustomizacoes(item, ctx);
        if (customizacao != null) return customizacao;

        return ResultadoEtapa.concluida(Map.of(
                "tipo", "CRIAR", "idItem", item.getId(), "quantidades", resultado));
    }

    private ResultadoEtapa dispararCustomizacoes(EntidadeInstancia item, ExecucaoContexto ctx) {
        JsonNode propriedades = item.getEntidadeSistema().getPropriedades();
        if (propriedades == null || propriedades.isMissingNode()) return null;

        for (Map.Entry<String, JsonNode> entry : propriedades.properties()) {
            if (!entry.getKey().startsWith("tabela_customizacao_")) continue;

            String chaveTabela = entry.getKey();
            String chaveRolagem = "customizacao_" + item.getId() + "_" + chaveTabela;
            String chaveConcluida = chaveRolagem + "_concluida";

            if (ctx.getContexto().containsKey(chaveConcluida)) continue;

            EtapaProcedimento sub = new EtapaProcedimento();
            sub.setTipoEtapa("CUSTOMIZAR_ENTIDADE");
            sub.setNome("Customizar " + item.getEntidadeSistema().getNome() + " — " + chaveTabela);
            sub.setParametrosEtapa(mapper.valueToTree(Map.of(
                    "id_entidade_sistema", item.getEntidadeSistema().getId(),
                    "chave_rolagem",       chaveRolagem,
                    "chave_tabela",        chaveTabela
            )));

            ResultadoEtapa resultado = handlers.get("CUSTOMIZAR_ENTIDADE").executar(sub, ctx);

            if (resultado.tipo() == ResultadoEtapa.Tipo.AGUARDANDO_INPUT
            || resultado.tipo() == ResultadoEtapa.Tipo.ERRO) {
                return resultado;
            }

            ctx.getContexto().put(chaveConcluida, true);
        }
        return null;
    }

    // ── REMOVER: decrement quantity, delete relation only when it reaches zero ──

    private ResultadoEtapa remover(List<EntidadeInstancia> personagens,
                                   EntidadeInstancia item,
                                   Map<String, Object> params) {
        int qtd = resolverQtd(params, null); // qtd to remove must be explicit/static here
        Map<Long, Integer> resultado = new LinkedHashMap<>();

        for (EntidadeInstancia personagem : personagens) {
            EntidadeRelacao rel = itemResolver
                    .buscarRelacao(personagem.getId(), item.getId())
                    .orElse(null);

            if (rel == null) {
                resultado.put(personagem.getId(), 0);
                continue;
            }

            int restante = rel.getQuantidade() - qtd;
            if (restante <= 0) {
                entidadeRelacaoRepo.delete(rel);
                resultado.put(personagem.getId(), 0);
            } else {
                rel.setQuantidade(restante);
                entidadeRelacaoRepo.save(rel);
                resultado.put(personagem.getId(), restante);
            }
        }

        return ResultadoEtapa.concluida(Map.of(
                "tipo", "REMOVER", "idItem", item.getId(), "quantidades", resultado));
    }

    // ── EQUIPAR: toggle relation.customizacoes.equipado, enforcing slot exclusivity ──

    private ResultadoEtapa equipar(List<EntidadeInstancia> personagens,
                                   EntidadeInstancia item,
                                   ExecucaoContexto ctx) {
        String slot = itemResolver.slotDoItem(item);
        if (slot == null) {
            return ResultadoEtapa.erro("Item " + item.getId() + " não possui slot — não pode ser equipado");
        }

        for (EntidadeInstancia personagem : personagens) {
            EntidadeRelacao rel = itemResolver
                    .buscarRelacao(personagem.getId(), item.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Personagem " + personagem.getId() + " não possui o item " + item.getId()));

            // Unequip whatever currently occupies this slot
            itemResolver.buscarEquipadoPorSlot(personagem.getId(), slot)
                    .ifPresent(atual -> {
                        if (!atual.getEntidadeFilha().getId().equals(item.getId())) {
                            setEquipado(atual, false);
                            itemEfeitoService.dispararGatilho(
                                    atual.getEntidadeFilha(), "AO_DESEQUIPAR", personagem, personagem, Map.of());
                        }
                    });

            setEquipado(rel, true);
            itemEfeitoService.dispararGatilho(item, "AO_EQUIPAR", personagem, personagem, Map.of());
        }

        return ResultadoEtapa.concluida(Map.of(
                "tipo", "EQUIPAR", "idItem", item.getId(), "slot", slot));
    }

    private ResultadoEtapa desequipar(List<EntidadeInstancia> personagens,
                                      EntidadeInstancia item,
                                      ExecucaoContexto ctx) {
        for (EntidadeInstancia personagem : personagens) {
            EntidadeRelacao rel = itemResolver
                    .buscarRelacao(personagem.getId(), item.getId())
                    .orElseThrow();

            setEquipado(rel, false);
            itemEfeitoService.dispararGatilho(item, "AO_DESEQUIPAR", personagem, personagem, Map.of());
        }

        return ResultadoEtapa.concluida(Map.of("tipo", "DESEQUIPAR", "idItem", item.getId()));
    }

    private void setEquipado(EntidadeRelacao rel, boolean valor) {
        ObjectNode custom = rel.getCustomizacoes() instanceof ObjectNode on
                ? on : mapper.createObjectNode();
        custom.put("equipado", valor);
        rel.setCustomizacoes(custom);
        entidadeRelacaoRepo.save(rel);
    }

    // ── Param resolution helpers ─────────────────────────────────

    private Long resolverIdItem(Map<String, Object> params, ExecucaoContexto ctx) {
        String fonte  = (String) params.get("opcao_fonte_item");
        String opcao  = (String) params.get("opcao_item");
        if ("estatico".equals(fonte)) return Long.parseLong(opcao);
        if ("contexto".equals(fonte)) return ctx.getContexto().getLong(opcao).orElseThrow();
        throw new IllegalArgumentException("opcao_fonte_item inválida: " + fonte);
    }

    private int resolverQtd(Map<String, Object> params, ExecucaoContexto ctx) {
        String fonte = (String) params.get("fonte_qtd");
        String opcao = (String) params.get("opcao_qtd");
        if ("estatico".equals(fonte)) return Integer.parseInt(opcao);
        if ("contexto".equals(fonte)) return ctx.getContexto().getInt(opcao).orElseThrow();
        throw new IllegalArgumentException("fonte_qtd inválida: " + fonte);
    }

    private List<EntidadeInstancia> resolverPersonagens(Map<String, Object> params,
                                                        ExecucaoContexto ctx) {
        String fonte = (String) params.get("opcao_fonte_personagem");
        if ("instancia_ativa".equals(fonte)) return List.of(instanciaResolver.retornarAtiva(ctx));
        if (fonte.startsWith("batalha.")) return instanciaResolver.resolverDeFonte(fonte, ctx);
        throw new IllegalArgumentException("opcao_fonte_personagem inválida: " + fonte);
    }
}
