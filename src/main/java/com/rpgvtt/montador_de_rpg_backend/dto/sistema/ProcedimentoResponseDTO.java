// ProcedimentoResponseDTO.java
package com.rpgvtt.montador_de_rpg_backend.dto.sistema;

import tools.jackson.databind.JsonNode;
import java.util.List;

public record ProcedimentoResponseDTO(
        Long id,
        Long sistemaId,
        String sistemaNome,
        String nome,
        String descricao,
        String tipo,
        JsonNode configsGeral,
        List<EtapaProcedimentoResponseDTO> etapas  // sempre retorna as etapas junto
) {}