package com.rpgvtt.montador_de_rpg_backend.domain.validation.exception;

public class ValidacaoSchemaException extends RuntimeException {

    private final String campo;

     public ValidacaoSchemaException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }

}
