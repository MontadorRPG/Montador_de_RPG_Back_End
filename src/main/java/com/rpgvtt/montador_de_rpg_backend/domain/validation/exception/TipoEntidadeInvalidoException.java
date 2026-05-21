package com.rpgvtt.montador_de_rpg_backend.domain.validation.exception;

public class TipoEntidadeInvalidoException extends RuntimeException {

    public TipoEntidadeInvalidoException(String tipo) {
        super(
            "O tipo '%s' não existe no schema_entidades deste sistema".formatted(tipo)
        );
    }

}
