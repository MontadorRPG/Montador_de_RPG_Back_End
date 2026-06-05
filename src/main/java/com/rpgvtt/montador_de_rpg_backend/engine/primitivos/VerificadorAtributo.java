package com.rpgvtt.montador_de_rpg_backend.engine.primitivos;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeSistemaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.SistemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class VerificadorAtributo {

    private final EntidadeSistemaRepository entidadeSistemaRepo;
    private final SistemaRepository sistemaRepo;

    public boolean verificarAtributo (Long idSistema, String atributo, double valor){
        Sistema sistema = sistemaRepo.findById(idSistema)
                .orElseThrow(() -> new EntityNotFoundException(Sistema.class, idSistema));
        JsonNode schemaAtributo = sistema.getSchemaAtributos().get(atributo);
        int min = schemaAtributo.get("max").asInt();
        int max = schemaAtributo.get("min").asInt();
        return !(valor < min) && !(valor > max);
    }
}
