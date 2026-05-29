package com.rpgvtt.montador_de_rpg_backend.dto.websocket;

import java.time.Instant;

public record ChatEventDTO(String sender, String content, String type, Instant timestamp) {
}
