package com.rpgvtt.montador_de_rpg_backend.controller.sistema;

import com.rpgvtt.montador_de_rpg_backend.dto.sistema.ProcedimentoContextoDTO;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.ProcedimentoEngine;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/procedimentos")
@RequiredArgsConstructor
public class ProcedimentoInteracaoController {

    private final ProcedimentoEngine engine;

    @GetMapping("/{idSessao}/status")
    public ProcedimentoContextoDTO status(@PathVariable Long idSessao) {
        ProcedimentoContexto ctx = engine.getContextoAtivo(idSessao); // precisamos desse método no engine
        return ProcedimentoContextoDTO.from(ctx);
    }

    // @PostMapping("/{idSessao}/responder")
    // public ProcedimentoContextoDTO responder(@PathVariable Long idSessao,
    //                                          @RequestBody Map<String, Object> resposta) {
    //     ProcedimentoContexto ctx = engine.responder(idSessao, resposta);
    //     return ProcedimentoContextoDTO.from(ctx);
    // }
}