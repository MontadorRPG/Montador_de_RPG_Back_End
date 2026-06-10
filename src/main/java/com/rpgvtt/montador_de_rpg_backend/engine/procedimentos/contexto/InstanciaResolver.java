package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto;

import com.rpgvtt.montador_de_rpg_backend.domain.model.batalha.BatalhaParticipantes;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.batalha.Batalha;
import com.rpgvtt.montador_de_rpg_backend.repository.batalha.BatalhaParticipantesRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.batalha.BatalhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InstanciaResolver {

    private final EntidadeInstanciaRepository instanciaRepo;
    private final BatalhaRepository batalhaRepo;
    private final BatalhaParticipantesRepository batalhaParticipantesRepo;

    public List<EntidadeInstancia> retornarTodas(ProcedimentoContexto ctx) {
        List<Long> ids = ctx.idsInstancias();
        if (ids.isEmpty()) return List.of();

        // Single query for all IDs — no N+1
        List<EntidadeInstancia> instancias = instanciaRepo.findAllById(ids);

        // Preserve the order defined by the scope (matters for sequential area resolution)
        Map<Long, EntidadeInstancia> porId = instancias.stream()
                .collect(Collectors.toMap(
                        EntidadeInstancia::getId,
                        i -> i
                ));

        return ids.stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public EntidadeInstancia retornarAtiva(ProcedimentoContexto ctx) {
        return instanciaRepo
                .findById(ctx.idInstanciaAtiva())
                .orElseThrow(() -> new IllegalStateException(
                        "Instância não encontrada: " + ctx.idInstanciaAtiva()));
    }

    public EntidadeInstancia retornarDeContexto(ProcedimentoContexto ctx, String chave) {
        Long id = ctx.getContexto().getLong(chave).orElseThrow();
        return instanciaRepo
                .findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Instância não encontrada: " + id));
    }

    public void salvar(EntidadeInstancia instancia) {
        instanciaRepo.save(instancia);
    }

    /**
     * Extended resolverDeFonte — new fontes available when a batalha
     * is stored in contexto["id_batalha"]:
     *   "batalha.todos"
     *     → all active BatalhaParticipantes regardless of side
     *   "batalha.aliados"
     *     → same time as the current active instance
     *   "batalha.inimigos"
     *     → opposite time from the current active instance
     *   "batalha.time.<N>"
     *     → specific team index, e.g. "batalha.time.0"
     */
    public List<EntidadeInstancia> resolverDeFonte(String fonte,
                                                   ProcedimentoContexto ctx) {
        // ... existing cases ...

        if (fonte.startsWith("batalha.")) {
            return resolverDeBatalha(fonte, ctx);
        }

        throw new IllegalArgumentException("Fonte desconhecida: " + fonte);
    }

    private List<EntidadeInstancia> resolverDeBatalha(String fonte,
                                                      ProcedimentoContexto ctx) {
        Long idBatalha = ctx.getContexto()
                .getLongOrThrow("id_batalha");

        Batalha batalha = batalhaRepo.findById(idBatalha)
                .orElseThrow(() -> new IllegalStateException(
                        "Batalha não encontrada: " + idBatalha));

        String sub = fonte.substring("batalha.".length()); // "todos" | "aliados" | "inimigos" | "time.N"

        List<BatalhaParticipantes> participantes = switch (sub) {
            case "todos" -> batalha.participantesAtivos();

            case "aliados" -> {
                int timeAtivo = resolverTimeAtivo(batalha, ctx);
                yield batalha.participantesLado(timeAtivo);
            }

            case "inimigos" -> {
                int timeAtivo = resolverTimeAtivo(batalha, ctx);
                // all active participants NOT on the executor's side
                yield batalha.participantesAtivos().stream()
                        .filter(p -> p.getLado() != timeAtivo)
                        .toList();
            }

            default -> {
                if (sub.startsWith("time.")) {
                    int timeIdx = Integer.parseInt(sub.substring("time.".length()));
                    yield batalha.participantesLado(timeIdx);
                }
                throw new IllegalArgumentException("Sub-fonte de batalha desconhecida: " + sub);
            }
        };

        // Bulk fetch instances — single query
        List<Long> ids = participantes.stream()
                .map(p -> p.getEntidadeInstancia().getId())
                .toList();

        return preservarOrdem(ids, instanciaRepo.findAllById(ids));
    }

    private int resolverTimeAtivo(Batalha batalha, ProcedimentoContexto ctx) {
        Long idAtiva = ctx.idInstanciaAtiva();
        return batalha.participantesAtivos().stream()
                .filter(p -> p.getEntidadeInstancia().getId().equals(idAtiva))
                .map(BatalhaParticipantes::getLado)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Instância ativa não encontrada na batalha"));
    }

    private List<EntidadeInstancia> preservarOrdem(List<Long> ids,
                                                   List<EntidadeInstancia> instancias) {
        Map<Long, EntidadeInstancia> porId = instancias.stream()
                .collect(Collectors.toMap(EntidadeInstancia::getId, i -> i));
        return ids.stream().map(porId::get).filter(Objects::nonNull).toList();
    }
}
