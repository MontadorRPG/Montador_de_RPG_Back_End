package com.rpgvtt.montador_de_rpg_backend.service.exceptions;

public class DeniedAcessException extends RuntimeException {
    public DeniedAcessException(String message) {
        super(message);
    }
}
