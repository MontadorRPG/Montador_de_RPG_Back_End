package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Cena;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.interpretador.contexto.Contexto;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaRepository;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class InterpretadorContexto implements Contexto {

    private final ProcedimentoContexto ctx;
    private final InstanciaResolver    instanciaResolver;
    private final CenaRepository       cenaRepo;
    private final JsonMapper           mapper;

    // Lazy caches
    private List<EntidadeInstancia> inimigosCache;
    private List<EntidadeInstancia> aliadosCache;
    private List<EntidadeInstancia> participantesCache;


    /**
     * Paths exposed:
     *
     *   iteracao                  → int
     *
     *   batalha.rodada            → int
     *   batalha.status            → String
     *
     *   contexto.<chave>          → any value in ProcedimentoContexto.contexto
     *
     *   instancia.<atributo>      → value from atributos_atuais of active instance
     *
     *   inimigos                  → List<Map<String,Object>> (for caminho_coringa + filtro)
     *   aliados                   → same
     *   participantes             → same
     *
     *   inimigos.*                → same list (for caminho base path in filtro's lista)
     *   aliados.*                 → same
     *   participantes.*           → same
     */
    @Override
    public Optional<Object> get(String caminho) {
        try {
            return Optional.ofNullable(resolver(caminho));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Object resolver(String caminho) {

        if ("iteracao".equals(caminho)) {
            return ctx.getIteracaoAtual();
        }

        if (caminho.startsWith("cena.")) {
            return resolverCena(caminho.substring("cena.".length()));
        }

        if (caminho.startsWith("contexto.")) {
            String chave = caminho.substring("contexto.".length());
            return ctx.getContexto().get(chave, Object.class).orElse(null);
        }

        if (caminho.startsWith("instancia.")) {
            String atributo = caminho.substring("instancia.".length());
            if (!ctx.temInstanciaUnica()) return null;
            EntidadeInstancia inst = instanciaResolver.retornarAtiva(ctx);
            return inst.getAtributosAtuais().get(atributo);
        }

        if ("inimigos".equals(caminho) || "inimigos.*".equals(caminho)) {
            return asAtributosMap(getInimigos());
        }
        if ("aliados".equals(caminho) || "aliados.*".equals(caminho)) {
            return asAtributosMap(getAliados());
        }
        if ("participantes".equals(caminho) || "participantes.*".equals(caminho)) {
            return asAtributosMap(getParticipantes());
        }

        return null;
    }

    /**
      * Converts instances to a list of their atributos_atuais maps.
      * This is what filtro and caminho_coringa iterate over —
      * each "item" inside a condition is a Map<String, Object> of attributes.
      */
    private List<Map<String, Object>> asAtributosMap(List<EntidadeInstancia> instancias) {
        return instancias.stream()
                .map(i -> mapper.convertValue(
                        i.getAtributosAtuais(), new TypeReference<Map<String, Object>>() {}))
                .toList();
    }

    private Object resolverCena(String sub) {
        Long idCena = ctx.getContexto().getLong("id_cena").orElse(null);
        if (idCena == null) return null;
        Cena cena = cenaRepo.findById(idCena).orElse(null);
        if (cena == null) return null;
        JsonNode estado = cena.getEstado();
        if (estado == null) return null;

        return switch (sub) {
            case "rodada" -> estado.has("rodada") ? estado.get("rodada").asInt() : null;
            case "status" -> {
                if (estado.has("status")) {
                    yield estado.get("status").asText();
                }
                if (estado.has("combateAtivo")) {
                    yield estado.get("combateAtivo").asBoolean() ? "EM_ANDAMENTO" : "CONCLUIDO";
                }
                yield null;
            }
            // 🆕 Novos campos
            case "turnoAtualId" -> estado.has("turnoAtualId") && !estado.get("turnoAtualId").isNull()
                    ? estado.get("turnoAtualId").asLong() : null;
            case "jaAgiramIds" -> estado.has("jaAgiramIds")
                    ? converterJsonArrayParaListaDeLong(estado.get("jaAgiramIds")) : null;
            case "dadosDisponiveis" -> estado.has("dadosDisponiveis")
                    ? estado.get("dadosDisponiveis") : null;
            default -> null;
        };
    }

    // Método auxiliar para converter um JsonNode array em List<Long>
    private List<Long> converterJsonArrayParaListaDeLong(JsonNode arrayNode) {
        List<Long> lista = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode n : arrayNode) {
                if (n.isNumber()) lista.add(n.asLong());
            }
        }
        return lista;
    }

    private List<EntidadeInstancia> getInimigos() {
        if (inimigosCache == null)
            inimigosCache = instanciaResolver.resolverDeFonte("cena.inimigos", ctx);
        return inimigosCache;
    }

    private List<EntidadeInstancia> getAliados() {
        if (aliadosCache == null)
            aliadosCache = instanciaResolver.resolverDeFonte("cena.aliados", ctx);
        return aliadosCache;
    }

    private List<EntidadeInstancia> getParticipantes() {
        if (participantesCache == null)
            participantesCache = instanciaResolver.resolverDeFonte("cena.todos", ctx);
        return participantesCache;
    }
}
