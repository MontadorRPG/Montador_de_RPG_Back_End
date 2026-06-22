package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.HandlerRegistry;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.*;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto.Status;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EscopoInstancias;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EtapaHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProcedimentoEngine {

    private final SessaoContexto sessaoCtx;
    private final ProcedimentoLoader loader;
    private final InstanciaResolver instanciaResolver;
    private final JsonMapper mapper;
    private final HandlerRegistry handlers;

    public ProcedimentoEngine(SessaoContexto sessaoContexto,
                              ProcedimentoLoader loader,
                              InstanciaResolver instanciaResolver,
                              JsonMapper mapper,
                              HandlerRegistry handlers
                              ) {
        this.sessaoCtx = sessaoContexto;
        this.loader = loader;
        this.instanciaResolver = instanciaResolver;
        this.mapper = mapper;
        this.handlers = handlers;
    }

    public ProcedimentoContexto iniciarComInstancia(Long idProcedimento,
                                                    Long idSessao,
                                                    Long idInstancia) {
        ProcedimentoContexto raiz = loader.carregar(
                LoadRequest.raiz(
                        idProcedimento, idSessao,
                        EscopoInstancias.unica(idInstancia)
                )
        );
        sessaoCtx.empurrar(idSessao, raiz);
        return avancar(idSessao);
    }

    public ProcedimentoContexto iniciarComMultiplos(Long idProcedimento,
                                                  Long idSessao,
                                                  List<Long> ids) {
        ProcedimentoContexto raiz = loader.carregar(
                LoadRequest.raiz(
                        idProcedimento, idSessao,
                        EscopoInstancias.multiplas(ids)
                )
        );
        sessaoCtx.empurrar(idSessao, raiz);
        return avancar(idSessao);
    }

    public ProcedimentoContexto iniciarSemInstancia(Long idProcedimento,
                                                  Long idSessao) {
        ProcedimentoContexto raiz = loader.carregar(
                LoadRequest.semInstancia(idProcedimento, idSessao)
        );
        sessaoCtx.empurrar(idSessao, raiz);
        return avancar(idSessao);
    }

    public ProcedimentoContexto responder(Long idSessao, Object valor) {
        ProcedimentoContexto frame = sessaoCtx.frameAtivo(idSessao);
        EtapaProcedimento etapaPendente = frame.getEtapaPendente();
        if (etapaPendente == null) {
            throw new IllegalStateException("Nenhuma etapa aguardando input");
        }

        JsonNode params = etapaPendente.getParametrosEtapa();
        String chave = params.get("salvar_em").asString();

        // Armazena o valor como veio (String, Integer etc.)
        frame.getContexto().put(chave, valor);
        frame.setAguardandoInput(false);
        frame.setEtapaPendente(null);

        // NÃO avança manualmente – o avancar() fará isso ao reprocessar a etapa
        return avancar(idSessao);
    }


    private ProcedimentoContexto avancar(Long idSessao) {

        while (sessaoCtx.temProcedimentoAtivo(idSessao)) {
            ProcedimentoContexto frame = sessaoCtx.frameAtivo(idSessao);

            // ── This frame is done ──────────────────────────────
            if (frame.etapasConcluidas()) {
                frame.setStatus(Status.CONCLUIDO);
                ProcedimentoContexto pai = sessaoCtx.concluirFrame(idSessao);

                if (pai == null) {
                    // Root procedure finished — combat over
                    return frame;
                }

                // Write child result into parent context
                if (frame.getRetornoContexto() != null) {
                    pai.getContexto().put(
                            frame.getRetornoContexto(),
                            frame.getHistorico() // or a summary DTO
                    );
                }

                // Advance parent past the CHAMAR_PROCEDIMENTO etapa
                pai.avancarEtapa();
                continue;     // loop again with parent now on top
            }

            // ── Execute current etapa ────────────────────────────
            EtapaProcedimento etapa = frame.etapaCorrente();

            if (!etapa.isObrigatorio() && naoDisponivel(etapa, frame)) {
                frame.pularEtapa(etapa.getNome() + " — não disponível");
                continue;
            }

            EtapaHandler handler = handlers.get(etapa.getTipoEtapa());
            if (handler == null) {
                frame.pularEtapa("handler não encontrado: " + etapa.getTipoEtapa());
                continue;
            }

            ResultadoEtapa resultado = handler.executar(etapa, frame);
            frame.getHistorico().add(resultado);

            switch (resultado.tipo()) {

                case CONCLUIDA -> {
                    frame.avancarEtapa();
                }

                case AGUARDANDO_INPUT -> {
                    frame.setAguardandoInput(true);
                    frame.setEtapaPendente(etapa);
                    return frame;
                }

                case ERRO -> {
                    frame.setStatus(Status.ERRO);
                    return frame;
                }
            }
        }

        return null;
    }

    private boolean naoDisponivel(EtapaProcedimento etapa, ProcedimentoContexto ctx) {
        Map<String, Object> params = mapper.convertValue(etapa.getParametrosEtapa(), new TypeReference<>(){});

        String recursoNecessario = (String) params.get("recurso_necessario");
        if (recursoNecessario == null) return false;

        if (ctx.getEscopo() instanceof EscopoInstancias.Nenhuma) return true;

        EntidadeInstancia instancia = instanciaResolver.retornarAtiva(ctx);
        JsonNode val = instancia.getAtributosAtuais().get(recursoNecessario);

        if (val == null || val.isNull()) return true;
        if (val.isBoolean()) return !val.asBoolean();
        if (val.isNumber()) return val.longValue() <= 0;
        return true;
    }

    public ProcedimentoContexto getContextoAtivo(Long idSessao) {
        return sessaoCtx.frameAtivo(idSessao);
    }

}