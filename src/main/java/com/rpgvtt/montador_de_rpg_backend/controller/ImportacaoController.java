package com.rpgvtt.montador_de_rpg_backend.controller;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rpgvtt.montador_de_rpg_backend.dto.importacao.ImportacaoRequestDTO;
import com.rpgvtt.montador_de_rpg_backend.service.importacao.ImportacaoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ImportacaoController {

    
    private final ImportacaoService importacaoService;

    @PostMapping("/importar")
    public ResponseEntity<Map<String, Long>> importar(@RequestBody ImportacaoRequestDTO request) {
        Map<String, Long> ids = importacaoService.processar(request);
        return ResponseEntity.ok(ids);
    }
}
