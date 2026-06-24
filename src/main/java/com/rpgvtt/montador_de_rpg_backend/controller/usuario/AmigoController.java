package com.rpgvtt.montador_de_rpg_backend.controller.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rpgvtt.montador_de_rpg_backend.dto.usuario.AmizadeResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.usuario.AmizadeSolicitarDTO;
import com.rpgvtt.montador_de_rpg_backend.service.usuario.AmizadeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/amigos")
@RequiredArgsConstructor
public class AmigoController {

    private final AmizadeService amizadeService;

    @GetMapping
    public ResponseEntity<List<AmizadeResponseDTO>> listarAmigos(
            @RequestAttribute("idUsuario") Long idUsuario) {
        return ResponseEntity.ok(amizadeService.listarAmigos(idUsuario));
    }

    @PostMapping("/solicitar")
    public ResponseEntity<AmizadeResponseDTO> enviarSolicitacao(
            @RequestBody AmizadeSolicitarDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(amizadeService.enviarSolicitacao(dto));
    }

    @PostMapping("/aceitar")
    public ResponseEntity<AmizadeResponseDTO> aceitarSolicitacao(
            @RequestParam Long remetenteId,
            @RequestParam Long destinatarioId) {
        return ResponseEntity.ok(amizadeService.aceitarSolicitacao(remetenteId, destinatarioId));
    }

    @DeleteMapping("/{idAmigo}")
    public ResponseEntity<Void> removerAmigo(
            @PathVariable Long idAmigo,
            @RequestAttribute("idUsuario") Long idUsuario) {
        amizadeService.remover(idUsuario, idAmigo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pendentes")
    public ResponseEntity<List<AmizadeResponseDTO>> listarPendentes(
            @RequestAttribute("idUsuario") Long idUsuario) {
        return ResponseEntity.ok(amizadeService.listarSolicitacoesPendentes(idUsuario));
    }
}
