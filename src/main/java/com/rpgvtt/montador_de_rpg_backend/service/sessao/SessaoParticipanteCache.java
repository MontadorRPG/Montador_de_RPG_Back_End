package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SessaoParticipanteCache {

    // idSessao → connected participants
    private final ConcurrentHashMap<Long, List<ParticipanteSessao>> salas = new ConcurrentHashMap<>();
    // idSessao → pending invites
    private final ConcurrentHashMap<Long, List<Convite>> convites = new ConcurrentHashMap<>();

    public void inicializar(Long idSessao, Long idMestre) {
        salas.put(idSessao, new CopyOnWriteArrayList<>(List.of(
                new ParticipanteSessao(idMestre, true, null)
        )));
        convites.put(idSessao, new CopyOnWriteArrayList<>());
    }

    public void adicionar(Long idSessao, ParticipanteSessao participante) {
        salas.computeIfAbsent(idSessao, k -> new CopyOnWriteArrayList<>())
                .removeIf(p -> p.idUsuario().equals(participante.idUsuario())); // dedupe reconnects
        salas.get(idSessao).add(participante);
    }

    public void remover(Long idSessao, Long idUsuario) {
        salas.getOrDefault(idSessao, List.of())
                .removeIf(p -> p.idUsuario().equals(idUsuario));
    }

    public List<ParticipanteSessao> listar(Long idSessao) {
        return List.copyOf(salas.getOrDefault(idSessao, List.of()));
    }

    public boolean isMestre(Long idSessao, Long idUsuario) {
        return salas.getOrDefault(idSessao, List.of()).stream()
                .anyMatch(p -> p.idUsuario().equals(idUsuario) && p.mestre());
    }

    public void registrarConvite(Long idSessao, Long idUsuario,
                                 String token, LocalDateTime expira) {
        convites.computeIfAbsent(idSessao, k -> new CopyOnWriteArrayList<>())
                .removeIf(c -> c.idUsuario().equals(idUsuario)); // replace old invite
        convites.get(idSessao).add(new Convite(idUsuario, token, expira));
    }

    public Optional<Convite> consumirConvite(Long idSessao, Long idUsuario, String token) {
        List<Convite> lista = convites.getOrDefault(idSessao, List.of());
        Optional<Convite> convite = lista.stream()
                .filter(c -> c.idUsuario().equals(idUsuario)
                        && c.token().equals(token)
                        && LocalDateTime.now().isBefore(c.expira()))
                .findFirst();
        convite.ifPresent(lista::remove); // single-use
        return convite;
    }

    public void limpar(Long idSessao) {
        salas.remove(idSessao);
        convites.remove(idSessao);
    }

    /**
     * Returns the IDs of known session rooms in the cache.
     */
    public java.util.Set<Long> listarIds() {
        return java.util.Set.copyOf(salas.keySet());
    }
}

