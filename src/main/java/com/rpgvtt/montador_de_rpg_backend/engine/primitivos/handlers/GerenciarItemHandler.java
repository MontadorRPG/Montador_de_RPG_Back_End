package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;


import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeRelacao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeRelacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GerenciarItemHandler implements EtapaHandler {

    private final JsonMapper mapper;
    private final InstanciaResolver instanciaResolver;
    private final EntidadeRelacaoRepository entidadeRelacaoRepo;
    private final EntidadeInstanciaRepository entidadeInstanciaRepo;

    @Override
    public String tipoEtapa() {
        return "GERENCIAR_ITEM";
    }

    @Override
    public ResultadoEtapa executar(EtapaProcedimento etapa, ProcedimentoContexto ctx) {
        Map<String, Object> params = mapper.convertValue(etapa.getParametrosEtapa(), new TypeReference<>() {});
        String tipo = (String) params.get("tipo");
        String fonteItem = (String) params.get("opcao_fonte_item"); //  "estatico", "contexto"
        String opcaoItem = (String) params.get("opcao_item"); // idItem direto se for estatico, chave do contexto se for contexto
        String fonteQtd = (String) params.get("fonte_qtd"); // "estatico", "contexto"
        String opcaoQtd = (String) params.get("opcao_qtd");
        String fontePersonagem = (String) params.get("opcao_fonte_personagem"); // "instancia_ativa", "batalha.aliados", "batalha.inimigos", "todos"

        Long idItem = null;

        if (fonteItem.startsWith("estatico")) idItem = Long.parseLong(opcaoItem);
        if (fonteItem.startsWith("contexto")) idItem = ctx.getContexto().getLong(opcaoItem).orElseThrow();
        if (idItem == null) throw new RuntimeException();

        List<EntidadeInstancia> personagens = new ArrayList<>();

        if (fontePersonagem.startsWith("instancia_ativa")) personagens = List.of(instanciaResolver.retornarAtiva(ctx));
        if (fontePersonagem.startsWith("batalha.")) personagens = instanciaResolver.resolverDeFonte(fontePersonagem, ctx);
        if (personagens.isEmpty()) throw new RuntimeException();

        Integer qtd = null;

        if (fonteQtd.startsWith("estatico")) qtd = Integer.parseInt(opcaoQtd);
        if (fonteQtd.startsWith("contexto")) qtd = ctx.getContexto().getInt(opcaoQtd).orElseThrow();
        if (qtd == null) throw new RuntimeException();

        switch (tipo) {
            case "CRIAR" -> {
                EntidadeInstancia instanciaItem = entidadeInstanciaRepo.findById(idItem).orElseThrow();
                for (EntidadeInstancia personagem : personagens) {
                    EntidadeRelacao entidadeRelacao = new EntidadeRelacao();
                    entidadeRelacao.setIdEntidadeFilha(instanciaItem);
                    entidadeRelacao.setIdEntidadePai(personagem);
                    entidadeRelacao.setQuantidade(qtd);
                    entidadeRelacaoRepo.save(entidadeRelacao);
                }
            }
            case "REMOVER" -> {
                for (EntidadeInstancia personagem : personagens) {
                    EntidadeRelacao entidadeRelacao = entidadeRelacaoRepo
                            .findByIdEntidadePaiAndIdEntidadeFilho(personagem.getId(), idItem);
                    entidadeRelacaoRepo.delete(entidadeRelacao);
                }
            }
            // No futuro, fazer case "ALTERAR" quando terminar as customizações
        }

        return null;
    }
}
