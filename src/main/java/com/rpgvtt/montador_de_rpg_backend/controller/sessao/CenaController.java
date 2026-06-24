package com.rpgvtt.montador_de_rpg_backend.controller.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Cena;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.service.sessao.CenaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cenas")
@RequiredArgsConstructor
public class CenaController {

    private final CenaService service;

    @PostMapping
    public ResponseEntity<CenaResponseDTO> criar(@RequestBody CenaCreateDTO dto) {
        CenaResponseDTO created = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CenaResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/sessao/{idSessao}")
    public ResponseEntity<List<CenaResponseDTO>> listBySessao(@PathVariable Long idSessao) {
        return ResponseEntity.ok(service.listarPorSessao(idSessao));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CenaResponseDTO> atualizar(@PathVariable Long id,
                                                    @RequestBody CenaUpdateDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/sessoes/{idSessao}/cena")
    public ResponseEntity<CenaResponseDTO> cenaAtiva(@PathVariable Long idSessao) {
        Cena cena = service.buscarCenaAtiva(idSessao); // implemente no service
        return ResponseEntity.ok(service.toDTO(cena));
    }

}
