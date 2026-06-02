package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusSessao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessaoRehydrator {

    private final SessaoRepository sessaoRepo;
    private final SessaoContexto sessaoContexto;
    private final ProcedimentoLoader loader;
    private final ObjectMapper mapper;

    @EventListener(ApplicationReadyEvent.class)
    public void rehidratar(){
        List<Sessao> sessoesAtivas = sessaoRepo
                .findByStatusAndProcedimentoAtivoIsNotNull(StatusSessao.EM_ANDAMENTO);

        log.info("Retornando {} sessões ativas após reinício", sessoesAtivas.size());

        for (Sessao sessao : sessoesAtivas) {
            try {
                rehidratarSessao(sessao);
            } catch (Exception e) {
                log.error("Falha ao retornar sessão {}: {}",
                        sessao.getId(), e.getMessage());
                sessao.setStatus(StatusSessao.PAUSADA);
                sessaoRepo.save(sessao);
            }
        }
    }

    private void rehidratarSessao(Sessao sessao) throws Exception {
        SessaoSnapshot snapshot = mapper.treeToValue(
                sessao.getSessaoSnapshot(), SessaoSnapshot.class);

        List<ProcedimentoSnapshot> procSnapshots = snapshot.getPilha();

        Deque<ProcedimentoContexto> pilha = new ArrayDeque<>();
        ProcedimentoContexto frameAbaixo = null;
        for (int i = procSnapshots.size() - 1; i >= 0; i--) {
            ProcedimentoSnapshot snap = procSnapshots.get(i);
            EscopoInstancias escopo   = rehidratarEscopo(snap);

            ProcedimentoContexto ctx;

            if (frameAbaixo == null) {
                // Root frame
                ctx = loader.carregar(LoadRequest.raiz(
                        snap.getIdProcedimento(),
                        snap.getIdSessao(),
                        escopo
                ));
            } else {
                // Child frame — inherit participantes and sistema from parent below
                ctx = loader.carregar(LoadRequest.filho(
                        snap.getIdProcedimento(),
                        frameAbaixo,
                        escopo,
                        snap.getContextoRetorno(),
                        snap.getContexto() // restored from snapshot
                ));
            }

            // Restore cursor and pipeline state
            ctx.setEtapaAtual(snap.getEtapaAtual());
            ctx.setStatus(snap.getStatus());
            ctx.setContexto(snap.getContexto());
            ctx.setHistorico(snap.getHistorico());

            pilha.push(ctx); // push builds stack top-to-bottom
            frameAbaixo = ctx;
        }

        // Register the rebuilt stack
        // Push in reverse so top of original stack ends up on top
        Deque<ProcedimentoContexto> pilhaOrdenada = new ArrayDeque<>();
        new ArrayList<>(pilha).forEach(pilhaOrdenada::addLast);

        pilha.forEach(frame ->
                sessaoContexto.empurrarSemPersistir(sessao.getId(), frame));

        log.info("Sessão {} rehydratada com {} frames na pilha",
                sessao.getId(), procSnapshots.size());
    }

    private EscopoInstancias rehidratarEscopo(ProcedimentoSnapshot snap) {
        return switch (snap.getEscopoTipo()) {
            case "NENHUMA"   -> EscopoInstancias.nenhuma();
            case "UNICA"     -> EscopoInstancias.unica(snap.getEscopoIds().getFirst());
            case "MULTIPLAS" -> EscopoInstancias.multiplas(snap.getEscopoIds());
            default -> throw new IllegalStateException(
                    "Tipo de escopo desconhecido: " + snap.getEscopoTipo());
        };
    }
}
