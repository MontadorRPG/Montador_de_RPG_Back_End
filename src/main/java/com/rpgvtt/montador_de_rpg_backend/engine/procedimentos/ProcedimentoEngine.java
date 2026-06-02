package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.ProcedimentoContexto.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProcedimentoEngine {

    private final SessaoContexto sessaoCtx;
    private final ProcedimentoLoader loader;
    private final Map<String, EtapaHandler> handlers;
    private final InstanciaResolver instanciaResolver;
    private final ObjectMapper mapper;

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

    public ProcedimentoContexto responder(Long idSessao,
                                          Map<String, Object> inputJogador) {
        ProcedimentoContexto frame = sessaoCtx.frameAtivo(idSessao);

        frame.getContexto().putAll(inputJogador);
        frame.setAguardandoInput(false);
        frame.setEtapaPendente(null);

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

                case SUB_PROCEDIMENTO_INICIADO -> {
                    // ChamarProcedimentoHandler already pushed the child.
                    // Loop again — next iteration picks up the child from stack top.
                    // Parent cursor stays where it is until child completes.
                }

                case AGUARDANDO_INPUT -> {
                    // Stop here and return the frame asking for input.
                    // The frame is still on the stack — next responder() call resumes it.
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
        Map<String, Object> params = mapper.convertValue(etapa.getParametros_etapa(), new TypeReference<>(){});

        String recursoNecessario = (String) params.get("recurso_necessario");
        if (recursoNecessario == null) return false;

        if (ctx.getEscopo() instanceof EscopoInstancias.Nenhuma) return true;

        EntidadeInstancia instancia = instanciaResolver.retornarAtiva(ctx);
        Object val = instancia.getAtributosAtuais().get(recursoNecessario);

        if (val == null)              return true;  // resource absent
        if (val instanceof Boolean b) return !b;    // false = not available
        if (val instanceof Number n)  return n.longValue() <= 0; // zero = exhausted
        return true; // unknown type = treat as not available
    }
}