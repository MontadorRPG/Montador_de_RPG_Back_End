package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.EscopoInstancias;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.snapshot.ProcedimentoSnapshot;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.snapshot.SessaoSnapshot;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessaoContexto {

    private final ConcurrentHashMap<Long, Deque<ProcedimentoContexto>> sessoes
            = new ConcurrentHashMap<>();

    private final SessaoRepository sessaoRepo;
    private final JsonMapper mapper;

    public void iniciarSessao(Long idSessao) {
        sessoes.put(idSessao, new ArrayDeque<>());
    }

    public void empurrar(Long idSessao, ProcedimentoContexto ctx) {
        Deque<ProcedimentoContexto> pilha = sessoes.computeIfAbsent(idSessao, _ -> new ArrayDeque<>());
        pilha.push(ctx);
        persistirSnapshot(idSessao, pilha);
    }

    public ProcedimentoContexto concluirFrame(Long idSessao) {
        Deque<ProcedimentoContexto> pilha = exigirPilha(idSessao);
        pilha.pop();

        if (pilha.isEmpty()) {
            limparSnapshot(idSessao);
            return null;
        }

        persistirSnapshot(idSessao, pilha);
        return pilha.peek();
    }

    public ProcedimentoContexto frameAtivo(Long idSessao) {
        Deque<ProcedimentoContexto> pilha = sessoes.get(idSessao);
        if (pilha == null || pilha.isEmpty())
            throw new IllegalStateException("Nenhum procedimento ativo na sessão " + idSessao);
        return pilha.peek();
    }

    public boolean temProcedimentoAtivo(Long idSessao) {
        Deque<ProcedimentoContexto> pilha = sessoes.get(idSessao);
        return pilha != null && !pilha.isEmpty();
    }

    public int profundidade(Long idSessao) {
        Deque<ProcedimentoContexto> pilha = sessoes.get(idSessao);
        return pilha == null ? 0 : pilha.size();
    }

    private void persistirSnapshot(Long idSessao,
                                   Deque<ProcedimentoContexto> pilha) {
        try {
            SessaoSnapshot snapshot = new SessaoSnapshot();

            // Convert stack to list: front (top) first
            snapshot.setPilha(
                    pilha.stream()
                            .map(this::toSnapshot)
                            .collect(Collectors.toList())
            );

            Sessao sessao = sessaoRepo.findById(idSessao).orElseThrow();
            sessao.setSessaoSnapshot(
                    mapper.valueToTree(snapshot)
            );
            sessaoRepo.save(sessao);

        } catch (Exception e) {
            log.error("Falha ao persistir snapshot da sessão {}: {}", idSessao, e.getMessage());
        }
    }

    private void limparSnapshot(Long idSessao) {
        sessaoRepo.findById(idSessao).ifPresent(s -> {
            s.setSessaoSnapshot(null);
            sessaoRepo.save(s);
        });
    }

    private ProcedimentoSnapshot toSnapshot(ProcedimentoContexto ctx) {
        ProcedimentoSnapshot snap = new ProcedimentoSnapshot();
        snap.setIdProcedimento(ctx.getProcedimento().getId());
        snap.setIdSessao(ctx.getIdSessao());
        snap.setEtapaAtual(ctx.getEtapaAtual());
        snap.setStatus(ctx.getStatus());
        snap.setContextoRetorno(ctx.getRetornoContexto());
        snap.setContextoMap(ctx.getContexto().dados());
        snap.setHistorico(ctx.getHistorico());

        // Serialize scope
        switch (ctx.getEscopo()) {
            case EscopoInstancias.Nenhuma n -> {
                snap.setEscopoTipo("NENHUMA");
                snap.setEscopoIds(List.of());
            }
            case EscopoInstancias.Unica u -> {
                snap.setEscopoTipo("UNICA");
                snap.setEscopoIds(List.of(u.id()));
            }
            case EscopoInstancias.Multiplas m -> {
                snap.setEscopoTipo("MULTIPLAS");
                snap.setEscopoIds(m.ids());
            }
        }

        return snap;
    }

    private Deque<ProcedimentoContexto> exigirPilha(Long idSessao) {
        Deque<ProcedimentoContexto> pilha = sessoes.get(idSessao);
        if (pilha == null || pilha.isEmpty())
            throw new IllegalStateException(
                    "Nenhum procedimento ativo na sessão " + idSessao);
        return pilha;
    }

    public void empurrarSemPersistir(Long idSessao, ProcedimentoContexto ctx) {
        sessoes.computeIfAbsent(idSessao, id -> new ArrayDeque<>()).push(ctx);
    }
}
