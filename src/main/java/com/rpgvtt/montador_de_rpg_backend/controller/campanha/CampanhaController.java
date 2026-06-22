package com.rpgvtt.montador_de_rpg_backend.controller.campanha;

import com.rpgvtt.montador_de_rpg_backend.dto.campanha.*;
import com.rpgvtt.montador_de_rpg_backend.security.UsuarioPrincipal;
import com.rpgvtt.montador_de_rpg_backend.service.campanha.CampanhaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campanhas")
public class CampanhaController {

    private final CampanhaService campanhaService;

    public CampanhaController(CampanhaService campanhaService) {
        this.campanhaService = campanhaService;
    }

    @PostMapping("/{campanhaId}/jogadores")
    public ResponseEntity<CampanhaParticipanteResponseDTO> adicionarJogador(
            @PathVariable Long campanhaId,
            @RequestBody @Valid AdicionarJogadorDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campanhaService.adicionarJogador(campanhaId, dto));
    }

    @PostMapping
    public ResponseEntity<CampanhaResponseDTO> criar(@RequestBody @Valid CampanhaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campanhaService.criar(dto));
    }

    // Endpoint dedicado para criação de personagem — tudo em uma transação
    @PostMapping("/temporaria-com-sessao")
    public ResponseEntity<CampanhaSessaoTemporariaDTO> criarTemporariaComSessao(
            @RequestBody TemporariaRequest req,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campanhaService.criarTemporariaComSessao(req.sistemaId(), principal.getId()));
    }

    public record TemporariaRequest(Long sistemaId) {}

    @GetMapping("/{id}")
    public ResponseEntity<CampanhaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(campanhaService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<CampanhaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(campanhaService.listarTodas());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        campanhaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}