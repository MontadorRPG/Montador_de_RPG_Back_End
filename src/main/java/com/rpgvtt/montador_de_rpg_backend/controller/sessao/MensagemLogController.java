package com.rpgvtt.montador_de_rpg_backend.controller.sessao;

import com.rpgvtt.montador_de_rpg_backend.dto.sessao.MensagemCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.MensagemResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.service.sessao.MensagemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mensagens")
@RequiredArgsConstructor
public class MensagemLogController {

    private final MensagemLogService service;

    @PostMapping
    public ResponseEntity<MensagemResponseDTO> criar(@RequestBody MensagemCreateDTO dto) {
        MensagemResponseDTO created = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MensagemResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/sessao/{idSessao}")
    public ResponseEntity<List<MensagemResponseDTO>> listBySessao(@PathVariable Long idSessao) {
        return ResponseEntity.ok(service.listarPorSessao(idSessao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

}
