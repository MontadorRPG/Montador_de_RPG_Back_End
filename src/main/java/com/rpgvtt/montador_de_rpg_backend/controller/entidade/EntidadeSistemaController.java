package com.rpgvtt.montador_de_rpg_backend.controller.entidade;

import com.rpgvtt.montador_de_rpg_backend.dto.entidade.EntidadeSistemaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.entidade.EntidadeSistemaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.entidade.EntidadeSistemaUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.service.entidade.EntidadeSistemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entidades-sistema")
@RequiredArgsConstructor
public class EntidadeSistemaController {

    private final EntidadeSistemaService service;

    @PostMapping
    public ResponseEntity<EntidadeSistemaResponseDTO> criar(@RequestBody @Valid EntidadeSistemaCreateDTO dto) {
        EntidadeSistemaResponseDTO nova = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nova);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntidadeSistemaResponseDTO> buscarPorId(@PathVariable Long id) {
        EntidadeSistemaResponseDTO dto = service.buscarPorId(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/sistema/{sistemaId}")
    public ResponseEntity<List<EntidadeSistemaResponseDTO>> listarPorSistema(@PathVariable Long sistemaId) {
        List<EntidadeSistemaResponseDTO> lista = service.listarPorSistema(sistemaId);
        return ResponseEntity.ok(lista);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EntidadeSistemaResponseDTO> atualizar(@PathVariable Long id,
                                                                @RequestBody @Valid EntidadeSistemaUpdateDTO dto) {
        EntidadeSistemaResponseDTO atualizado = service.atualizar(id, dto);
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}