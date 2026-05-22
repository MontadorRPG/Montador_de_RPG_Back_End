package com.rpgvtt.montador_de_rpg_backend.controller.sessao;

import com.rpgvtt.montador_de_rpg_backend.dto.sessao.SessaoCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.SessaoResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.service.sessao.SessaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessoes")
@RequiredArgsConstructor
public class SessaoController {

    private final SessaoService sessaoService;

    /**
     * POST /api/sessoes - Abre/Cria uma nova sessão.
     */
    @PostMapping
    public ResponseEntity<SessaoResponseDTO> criarSessao(@RequestBody @Valid SessaoCreateDTO dto) {
        SessaoResponseDTO novaSessao = sessaoService.criarSessao(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaSessao);
    }

    /**
     * GET /api/sessoes/{id} - Obtém os detalhes de uma sessão.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessaoResponseDTO> buscarPorId(@PathVariable Long id) {
        SessaoResponseDTO sessao = sessaoService.buscarPorId(id);
        return ResponseEntity.ok(sessao);
    }

    /**
     * PUT /api/sessoes/{id}/encerrar - Finaliza uma sessão de jogo em andamento.
     */
    @PutMapping("/{id}/encerrar")
    public ResponseEntity<SessaoResponseDTO> encerrarSessao(@PathVariable Long id) {
        SessaoResponseDTO sessaoEncerrada = sessaoService.encerrarSessao(id);
        return ResponseEntity.ok(sessaoEncerrada);
    }
}