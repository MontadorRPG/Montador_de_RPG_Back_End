// domain/engine/primitivos/Suspensao.java
package com.rpgvtt.montador_de_rpg_backend.engine.primitivos;

import lombok.Getter;
import com.fasterxml.jackson.databind.JsonNode;

// Representa uma pausa na execução que precisa de input do front.
@Getter
public class Suspensao {

    public enum TipoSuspensao {
        ESCOLHA   // ex: front precisa apresentar opções ao jogador

    }

    private final TipoSuspensao tipo;

    private final String guardarComo;

    // Dados da suspensão. Ex: as opcoes para o tipo ESCOLHA
    private final JsonNode dados;

    public Suspensao(TipoSuspensao tipo, String guardarComo, JsonNode dados) {
        this.tipo = tipo;
        this.guardarComo = guardarComo;
        this.dados = dados;
    }
}