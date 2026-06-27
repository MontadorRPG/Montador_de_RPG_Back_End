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

    @PostMapping
    public ResponseEntity<CampanhaResponseDTO> criar(@RequestBody @Valid CampanhaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campanhaService.criar(dto));
    }

    @PostMapping("/temporaria-com-sessao")
    public ResponseEntity<CampanhaSessaoTemporariaDTO> criarTemporariaComSessao(
            @RequestBody TemporariaRequest req,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campanhaService.criarTemporariaComSessao(req.sistemaId(), principal.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampanhaResponseDTO> atualizar(
            @PathVariable Long id, 
            @RequestBody @Valid CampanhaUpdateDTO dto) {
        return ResponseEntity.ok(campanhaService.atualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampanhaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(campanhaService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<CampanhaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(campanhaService.listarTodas());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        
        campanhaService.deletar(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    // CampanhaController.java
    @DeleteMapping("/{id}/temporaria")
    public ResponseEntity<Void> deletarTemporaria(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        campanhaService.deletarTemporaria(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<CampanhaResponseDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(campanhaService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<CampanhaResponseDTO>> listarMinhas(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(campanhaService.listarPorUsuario(principal.getId()));
    }

    // ── Participantes ──────────────────────────────────────────────

    /**
     * Agora o AdicionarJogadorDTO deve conter o campo 'email' (String).
     * O Service deverá buscar o usuário pelo e-mail e realizar o vínculo.
     */
    @PostMapping("/{campanhaId}/jogadores")
    public ResponseEntity<CampanhaParticipanteResponseDTO> adicionarJogador(
            @PathVariable Long campanhaId,
            @RequestBody @Valid AdicionarJogadorDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campanhaService.adicionarJogador(campanhaId, dto));
    }

    @GetMapping("/{campanhaId}/jogadores")
    public ResponseEntity<List<CampanhaParticipanteResponseDTO>> listarParticipantes(
            @PathVariable Long campanhaId) {
        return ResponseEntity.ok(campanhaService.listarParticipantes(campanhaId));
    }

    @DeleteMapping("/{campanhaId}/jogadores/{usuarioId}")
    public ResponseEntity<Void> removerJogador(
            @PathVariable Long campanhaId,
            @PathVariable Long usuarioId,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        
        campanhaService.removerJogador(campanhaId, principal.getId(), usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{campanhaId}/minha-role")
    public ResponseEntity<CampanhaPapelDTO> obterMinhaRole(
            @PathVariable Long campanhaId,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        
        return campanhaService.obterMinhaRole(campanhaId, principal.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // ── Personagem do jogador nesta campanha ───────────────────────

    @GetMapping("/{campanhaId}/meu-personagem")
    public ResponseEntity<PersonagemCampanhaDTO> meuPersonagem(
            @PathVariable Long campanhaId,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return campanhaService.buscarPersonagemDoUsuario(campanhaId, principal.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{campanhaId}/vincular-personagem")
    public ResponseEntity<PersonagemCampanhaDTO> vincularPersonagem(
            @PathVariable Long campanhaId,
            @RequestBody VincularPersonagemRequest req,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(
                campanhaService.vincularPersonagem(campanhaId, req.instanciaId(), principal.getId()));
    }

    // ── Records de request ─────────────────────────────────────────

    public record TemporariaRequest(Long sistemaId) {}
    public record VincularPersonagemRequest(Long instanciaId) {}
}