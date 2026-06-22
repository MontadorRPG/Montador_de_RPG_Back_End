package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces;

import tools.jackson.databind.JsonNode;

public interface EtapaExecutavel {
    String getNome();
    String getTipoEtapa();
    JsonNode getParametrosEtapa();
    
}
