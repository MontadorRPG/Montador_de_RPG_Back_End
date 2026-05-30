package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
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
        Object val = ctx.getContexto().get(chave);
        if (val == null) throw new IllegalStateException(
                "Chave '" + chave + "' não encontrada no contexto do procedimento");

        Long id = (Long) val;
        return instanciaRepo
                .findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Instância não encontrada: " + id));
    }

    public void salvar(EntidadeInstancia instancia) {
        instanciaRepo.save(instancia);
    }
}
