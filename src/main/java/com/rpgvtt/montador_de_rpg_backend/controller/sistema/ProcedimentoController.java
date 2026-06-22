package com.rpgvtt.montador_de_rpg_backend.controller.sistema;

import com.rpgvtt.montador_de_rpg_backend.dto.sistema.EtapaProcedimentoCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.EtapaProcedimentoResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.EtapaProcedimentoUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.ProcedimentoContextoDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.ProcedimentoCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.ProcedimentoResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.ProcedimentoUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.RespostaEtapaDTO;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.ProcedimentoEngine;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.service.sistema.ProcedimentoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/procedimentos")
@RequiredArgsConstructor
public class ProcedimentoController {

    private final ProcedimentoEngine engine;
    private final ProcedimentoService service;

    // ---------- CRUD ----------
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProcedimentoResponseDTO criar(@Valid @RequestBody ProcedimentoCreateDTO dto) {
        return service.criar(dto);
    }

    @GetMapping("/{id}")
    public ProcedimentoResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping
    public List<ProcedimentoResponseDTO> listar(
        @RequestParam(required = false) Long sistemaId) {
        return service.listar(sistemaId);
    }

    @PutMapping("/{id}")
    public ProcedimentoResponseDTO atualizar(@PathVariable Long id,
                                             @Valid @RequestBody ProcedimentoUpdateDTO dto) {
        return service.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }

    @PostMapping("/{id}/responder")
    @Transactional
    public ProcedimentoContextoDTO responder(@PathVariable Long id,
                                            @RequestParam @NotNull Long idSessao,
                                            @Valid @RequestBody RespostaEtapaDTO dto) {
        ProcedimentoContexto ctx = engine.responder(idSessao, dto.getValor());
        return ProcedimentoContextoDTO.from(ctx);
    }

    // ------------------ Etapas ---‑-----------------

    @PostMapping("/{procedimentoId}/etapas")
    @ResponseStatus(HttpStatus.CREATED)
    public EtapaProcedimentoResponseDTO adicionarEtapa(
            @PathVariable Long procedimentoId,
            @Valid @RequestBody EtapaProcedimentoCreateDTO dto) {
        return service.adicionarEtapa(procedimentoId, dto);
    }

    @PutMapping("/{procedimentoId}/etapas/{etapaId}")
    public EtapaProcedimentoResponseDTO atualizarEtapa(
            @PathVariable Long procedimentoId,
            @PathVariable Long etapaId,
            @Valid @RequestBody EtapaProcedimentoUpdateDTO dto) {
        return service.atualizarEtapa(etapaId, dto);
    }

    @DeleteMapping("/{procedimentoId}/etapas/{etapaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarEtapa(
            @PathVariable Long procedimentoId,
            @PathVariable Long etapaId) {
        service.deletarEtapa(etapaId);
    }

    /**
     * Inicia um procedimento já persistido com um escopo de instância único.
     * Útil para criar personagens a partir do procedimento do sistema.
     */
    @PostMapping("/{id}/iniciar-com-instancia")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcedimentoContextoDTO iniciarComInstancia(@PathVariable Long id,
                                                       @RequestParam @NotNull Long idSessao,
                                                       @RequestParam @NotNull Long idInstancia) {

        ProcedimentoContexto ctx = engine.iniciarComInstancia(id, idSessao, idInstancia);
        return ProcedimentoContextoDTO.from(ctx);
    }

    /**
     * Inicia um procedimento sem instância (configurações, criação global etc.)
     */
    @PostMapping("/{id}/iniciar-sem-instancia")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcedimentoContextoDTO iniciarSemInstancia(@PathVariable Long id,
                                                       @RequestParam @NotNull Long idSessao) {

        ProcedimentoContexto ctx = engine.iniciarSemInstancia(id, idSessao);
        return ProcedimentoContextoDTO.from(ctx);
    }

    /**
     * Inicia um procedimento com múltiplas instâncias (ex: todos os participantes)
     */
    @PostMapping("/{id}/iniciar-com-multiplas")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcedimentoContextoDTO iniciarComMultiplas(@PathVariable Long id,
                                                       @RequestParam @NotNull Long idSessao,
                                                       @RequestBody java.util.List<Long> ids) {

        ProcedimentoContexto ctx = engine.iniciarComMultiplos(id, idSessao, ids);
        return ProcedimentoContextoDTO.from(ctx);
    }
}