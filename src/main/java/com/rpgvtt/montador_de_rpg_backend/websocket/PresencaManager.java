package com.rpgvtt.montador_de_rpg_backend.websocket;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PresencaManager {

    private final Map<Long, Set<String>> usuariosConectados = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        Principal user = event.getUser();
        if (user == null) return;
        Long userId = Long.parseLong(user.getName());
        String sessionId = event.getMessage().getHeaders().get("simpSessionId").toString();
        usuariosConectados.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        broadcastPresenca();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user == null) return;
        Long userId = Long.parseLong(user.getName());
        String sessionId = event.getSessionId();
        Set<String> sessoes = usuariosConectados.get(userId);
        if (sessoes != null) {
            sessoes.remove(sessionId);
            if (sessoes.isEmpty()) usuariosConectados.remove(userId);
        }
        broadcastPresenca();
    }

    private void broadcastPresenca() {
        List<Long> online = new ArrayList<>(usuariosConectados.keySet());
        messagingTemplate.convertAndSend("/topic/usuarios", (Object) Map.of("online", online));
    }
}
