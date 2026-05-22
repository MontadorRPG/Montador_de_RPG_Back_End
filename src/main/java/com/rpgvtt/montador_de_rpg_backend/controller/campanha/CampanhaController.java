package com.rpgvtt.montador_de_rpg_backend.controller.campanha;

import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.AdicionarJogadorDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaParticipanteResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.service.campanha.CampanhaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CampanhaParticipanteResponseDTO> adicionarJogador(@PathVariable Long campanhaId, @RequestBody @Valid AdicionarJogadorDTO dto) {
        CampanhaParticipanteResponseDTO participante = campanhaService.adicionarJogador(campanhaId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(participante);
    }

    @PostMapping
    public ResponseEntity<CampanhaResponseDTO> criar(@RequestBody @Valid CampanhaCreateDTO dto) {
        CampanhaResponseDTO novaCampanha = campanhaService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaCampanha);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampanhaResponseDTO> buscarPorId(@PathVariable Long id) {
        CampanhaResponseDTO dto = campanhaService.buscarPorId(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<CampanhaResponseDTO>> listarTodas() {
        List<CampanhaResponseDTO> lista = campanhaService.listarTodas();
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        campanhaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}