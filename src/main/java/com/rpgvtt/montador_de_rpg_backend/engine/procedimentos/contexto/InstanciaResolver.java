package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Cena;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.CenaParticipantes;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaParticipantesRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaRepository;
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
    private final CenaRepository cenaRepo;
    private final CenaParticipantesRepository cenaParticipantesRepo;

    public List<EntidadeInstancia> retornarTodas(ExecucaoContexto ctx) {
        List<Long> ids = ctx.idsInstancias();
        if (ids.isEmpty()) return List.of();

        List<EntidadeInstancia> instancias = instanciaRepo.findAllById(ids);
        Map<Long, EntidadeInstancia> porId = instancias.stream()
                .collect(Collectors.toMap(EntidadeInstancia::getId, i -> i));

        return ids.stream().map(porId::get).filter(Objects::nonNull).toList();
    }

    public EntidadeInstancia buscarPorId(Long id) {
        return instanciaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(EntidadeInstancia.class, id));
    }

    public EntidadeInstancia retornarAtiva(ExecucaoContexto ctx) {
        return instanciaRepo.findById(ctx.idInstanciaAtiva())
                .orElseThrow(() -> new IllegalStateException(
                        "Instância não encontrada: " + ctx.idInstanciaAtiva()));
    }

    public EntidadeInstancia retornarDeContexto(ExecucaoContexto ctx, String chave) {
        Long id = ctx.getContexto().getLong(chave).orElseThrow();
        return instanciaRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Instância não encontrada: " + id));
    }

    public void salvar(EntidadeInstancia instancia) {
        instanciaRepo.save(instancia);
    }

    /**
     * Extended resolverDeFonte – agora usa Cena em vez de Batalha.
     * A chave "id_cena" deve estar no contexto.
     * Fontes suportadas:
     *   "cena.todos"       → todos os participantes ativos da cena
     *   "cena.aliados"     → mesmo lado da instância ativa
     *   "cena.inimigos"    → lados diferentes da instância ativa
     *   "cena.time.<N>"    → lado específico, ex.: "cena.time.0"
     */
    public List<EntidadeInstancia> resolverDeFonte(String fonte,
                                                   ExecucaoContexto ctx) {
        if (fonte.startsWith("cena.")) {
            return resolverDeCena(fonte, ctx);
        }
        throw new IllegalArgumentException("Fonte desconhecida: " + fonte);
    }

    private List<EntidadeInstancia> resolverDeCena(String fonte,
                                                   ExecucaoContexto ctx) {
        Long idCena = ctx.getContexto().getLongOrThrow("id_cena");

        Cena cena = cenaRepo.findById(idCena)
                .orElseThrow(() -> new IllegalStateException(
                        "Cena não encontrada: " + idCena));

        String sub = fonte.substring("cena.".length()); // "todos" | "aliados" | "inimigos" | "time.N"

        List<CenaParticipantes> todosParticipantes = cenaParticipantesRepo.findByCena(cena);

        List<CenaParticipantes> selecionados = switch (sub) {
            case "todos" -> todosParticipantes;

            case "aliados" -> {
                int ladoAtivo = resolverLadoAtivo(cena, ctx, todosParticipantes);
                yield todosParticipantes.stream()
                        .filter(p -> p.getLado() == ladoAtivo)
                        .toList();
            }

            case "inimigos" -> {
                int ladoAtivo = resolverLadoAtivo(cena, ctx, todosParticipantes);
                yield todosParticipantes.stream()
                        .filter(p -> p.getLado() != ladoAtivo)
                        .toList();
            }

            default -> {
                if (sub.startsWith("time.")) {
                    int timeIdx = Integer.parseInt(sub.substring("time.".length()));
                    yield todosParticipantes.stream()
                            .filter(p -> p.getLado() == timeIdx)
                            .toList();
                }
                throw new IllegalArgumentException("Sub-fonte de cena desconhecida: " + sub);
            }
        };

        List<Long> ids = selecionados.stream()
                .map(p -> p.getEntidadeInstancia().getId())
                .toList();

        return preservarOrdem(ids, instanciaRepo.findAllById(ids));
    }

    private int resolverLadoAtivo(Cena cena, ExecucaoContexto ctx,
                                  List<CenaParticipantes> todosParticipantes) {
        Long idAtiva = ctx.idInstanciaAtiva();
        return todosParticipantes.stream()
                .filter(p -> p.getEntidadeInstancia().getId().equals(idAtiva))
                .map(CenaParticipantes::getLado)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Instância ativa não encontrada na cena"));
    }

    private List<EntidadeInstancia> preservarOrdem(List<Long> ids,
                                                   List<EntidadeInstancia> instancias) {
        Map<Long, EntidadeInstancia> porId = instancias.stream()
                .collect(Collectors.toMap(EntidadeInstancia::getId, i -> i));
        return ids.stream().map(porId::get).filter(Objects::nonNull).toList();
    }
}