package com.rpgvtt.montador_de_rpg_backend.controller.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Cena;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaCombateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.IniciarCenaRequestDTO;
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

    // ========== CRUD Básico ==========

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
        Cena cena = service.buscarCenaAtiva(idSessao);
        return ResponseEntity.ok(service.toDTO(cena));
    }

    // ========== Métodos de Gerenciamento Adicionados ==========

    /**
     * Inicia uma nova cena de combate com procedimentos de iniciativa e times mapeados.
     */
    @PostMapping("/combate/iniciar")
    public ResponseEntity<CenaCombateDTO> iniciarCenaCombate(@RequestBody IniciarCenaRequestDTO req,
                                                             @RequestHeader("X-Mestre-Id") Long idMestre) {
        CenaCombateDTO combate = service.iniciarCena(req, idMestre);
        return ResponseEntity.status(HttpStatus.CREATED).body(combate);
    }

    /**
     * Adiciona uma instância de personagem ou criatura a uma cena de combate em andamento.
     */
    @PostMapping("/{id}/participantes")
    public ResponseEntity<Void> adicionarParticipante(@PathVariable Long id,
                                                      @RequestParam Long idInstancia,
                                                      @RequestParam int lado,
                                                      @RequestHeader("X-Mestre-Id") Long idMestre) {
        service.adicionarParticipante(id, idInstancia, lado, idMestre);
        return ResponseEntity.ok().build();
    }

    /**
     * Remove uma instância (participante) da cena de combate especificada.
     */
    @DeleteMapping("/{id}/participantes/{idInstancia}")
    public ResponseEntity<Void> removerParticipante(@PathVariable Long id,
                                                    @PathVariable Long idInstancia,
                                                    @RequestHeader("X-Mestre-Id") Long idMestre) {
        service.removerParticipante(id, idInstancia, idMestre);
        return ResponseEntity.noContent().build();
    }

    /**
     * Encerra uma cena de combate ativa modificando o estado booleano interno.
     */
    @PostMapping("/{id}/encerrar")
    public ResponseEntity<Void> encerrarCena(@PathVariable Long id,
                                             @RequestParam String motivo,
                                             @RequestHeader("X-Mestre-Id") Long idMestre) {
        service.encerrarCena(id, motivo, idMestre);
        return ResponseEntity.ok().build();
    }

    /**
     * Atualiza as coordenadas bidimensionais X e Y do token de uma instância em uma sessão.
     */
    @PatchMapping("/sessoes/{idSessao}/tokens/{idInstancia}/posicao")
    public ResponseEntity<Void> atualizarPosicaoToken(@PathVariable Long idSessao,
                                                      @PathVariable Long idInstancia,
                                                      @RequestParam double x,
                                                      @RequestParam double y) {
        service.atualizarPosicaoToken(idSessao, idInstancia, x, y);
        return ResponseEntity.ok().build();
    }
}