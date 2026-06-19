package com.rpgvtt.montador_de_rpg_backend.controller.sistema;

import com.rpgvtt.montador_de_rpg_backend.dto.sistema.SistemaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.SistemaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.SistemaUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.service.sistema.SistemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sistemas")
@RequiredArgsConstructor
public class SistemaController {

    private final SistemaService sistemaService;

    @PostMapping
    public ResponseEntity<SistemaResponseDTO> criar(@RequestBody @Valid SistemaCreateDTO dto) {
        SistemaResponseDTO novoSistema = sistemaService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoSistema);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SistemaResponseDTO> buscarPorId(@PathVariable Long id) {
        SistemaResponseDTO dto = sistemaService.buscarPorId(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<SistemaResponseDTO>> listarTodos() {
        List<SistemaResponseDTO> lista = sistemaService.listarTodos();
        return ResponseEntity.ok(lista);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SistemaResponseDTO> atualizar(@PathVariable Long id,
                                                        @RequestBody @Valid SistemaUpdateDTO dto) {
        SistemaResponseDTO atualizado = sistemaService.atualizar(id, dto);
        return ResponseEntity.ok(atualizado);
    }

    @PatchMapping("/{id}/oficial")
    public ResponseEntity<SistemaResponseDTO> marcarComoOficial(@PathVariable Long id) {
        SistemaResponseDTO oficial = sistemaService.marcarComoOficial(id);
        return ResponseEntity.ok(oficial);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        sistemaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}