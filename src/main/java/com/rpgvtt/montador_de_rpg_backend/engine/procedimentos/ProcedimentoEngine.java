package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.ProcedimentoContexto.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProcedimentoEngine {

    private final SessaoContexto sessaoCtx;
    private final ProcedimentoLoader loader;
    private final Map<String, EtapaHandler> handlers;
    private final InstanciaResolver instanciaResolver;

    /** Start a procedure that has a single actor — most combat turns */
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
        return avancar(idSessao, null);
    }

    /** Start a procedure that affects multiple instances — area effects, initiative */
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
        return avancar(idSessao, null);
    }

    /** Start a procedure with no instance scope — session setup, cleanup */
    public ProcedimentoContexto iniciarSemInstancia(Long idProcedimento,
                                                  Long idSessao) {
        ProcedimentoContexto raiz = loader.carregar(
                LoadRequest.semInstancia(idProcedimento, idSessao)
        );
        sessaoCtx.empurrar(idSessao, raiz);
        return avancar(idSessao, null);
    }

    private ProcedimentoContexto avancar(Long idSessao, Map<String, Object> input) {

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
                input = null; // parent resumes without input
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

            ResultadoEtapa resultado = handler.executar(etapa, frame, input);
            frame.getHistorico().add(resultado);

            switch (resultado.tipo()) {

                case CONCLUIDA -> {
                    frame.avancarEtapa();
                    input = null;
                }

                case SUB_PROCEDIMENTO_INICIADO -> {
                    // ChamarProcedimentoHandler already pushed the child.
                    // Loop again — next iteration picks up the child from stack top.
                    // Parent cursor stays where it is until child completes.
                    input = null;
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
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> params = mapper.convertValue(
                etapa.getParametros_etapa(), new TypeReference<>() {});
        if (!params.containsKey("requer_recurso")) return false;

        String recurso = (String) params.get("requer_recurso");

        List<EntidadeInstancia> instancias = instanciaResolver.retornarTodas(ctx);

        Map<String, Object> attrs = ;
        Object val = attrs.get(recurso);

        if (val instanceof Boolean b) return !b;
        if (val instanceof Number n)  return n.intValue() <= 0;
        return true; // recurso ausente = não disponível
    }
}