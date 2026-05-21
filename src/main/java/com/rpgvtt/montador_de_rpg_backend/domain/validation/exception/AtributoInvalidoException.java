package com.rpgvtt.montador_de_rpg_backend.domain.validation.exception;

public class AtributoInvalidoException extends RuntimeException {

    public AtributoInvalidoException(String nomeAtributo, String motivo) {
        super(
            "Atributo '%s' inválido: %s".formatted(nomeAtributo, motivo)
        );
    }

}
