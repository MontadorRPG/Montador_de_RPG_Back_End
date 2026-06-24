// ProcedimentoService.java
package com.rpgvtt.montador_de_rpg_backend.service.sistema;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusProcedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.dto.sistema.*;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.EtapaProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.ProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.SistemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcedimentoService {

    private final ProcedimentoRepository procedimentoRepository;
    private final EtapaProcedimentoRepository etapaRepository;
    private final SistemaRepository sistemaRepository;

    // [ ---------------- Procedimento ---------------- ]

    @Transactional
    public ProcedimentoResponseDTO criar(ProcedimentoCreateDTO dto) {
        Sistema sistema = sistemaRepository.findById(dto.sistemaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sistema não encontrado."));

        Procedimento procedimento = new Procedimento();
        procedimento.setSistema(sistema);
        procedimento.setNome(dto.nome());
        procedimento.setDescricao(dto.descricao());
        procedimento.setTipo(dto.tipo());
        procedimento.setConfigsGeral(dto.configsGeral());
        procedimento.setStatus(StatusProcedimento.SESSAO_ATIVA);

        return mapearParaDTO(procedimentoRepository.save(procedimento));
    }

    @Transactional(readOnly = true)
    public ProcedimentoResponseDTO buscarPorId(Long id) {
        Procedimento procedimento = procedimentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Procedimento não encontrado."));
        return mapearParaDTO(procedimento);
    }

    @Transactional(readOnly = true)
    public List<ProcedimentoResponseDTO> listar(Long sistemaId) {
        List<Procedimento> procedimentos;

        if (sistemaId != null) {
            if (!sistemaRepository.existsById(sistemaId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sistema não encontrado.");
            }
            procedimentos = procedimentoRepository.findBySistemaId(sistemaId);
        } else {
            // Se não informar sistemaId, traz todos os procedimentos
            procedimentos = procedimentoRepository.findAll();
        }

        return procedimentos.stream()
                .map(this::mapearParaDTO)
                .toList();
    }

    @Transactional
    public ProcedimentoResponseDTO atualizar(Long id, ProcedimentoUpdateDTO dto) {
        Procedimento procedimento = procedimentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Procedimento não encontrado."));

        if (dto.nome() != null)        procedimento.setNome(dto.nome());
        if (dto.descricao() != null)   procedimento.setDescricao(dto.descricao());
        if (dto.tipo() != null)        procedimento.setTipo(dto.tipo());
        if (dto.configsGeral() != null) procedimento.setConfigsGeral(dto.configsGeral());

        return mapearParaDTO(procedimentoRepository.save(procedimento));
    }

    @Transactional
    public void deletar(Long id) {
        if (!procedimentoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Procedimento não encontrado.");
        }
        // CascadeType.ALL no model garante que as etapas são deletadas junto
        procedimentoRepository.deleteById(id);
    }

    // [ ---------------- EtapaProcedimento ---------------- ]

    @Transactional
    public EtapaProcedimentoResponseDTO adicionarEtapa(Long procedimentoId, EtapaProcedimentoCreateDTO dto) {
        Procedimento procedimento = procedimentoRepository.findById(procedimentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Procedimento não encontrado."));

        boolean ordemOcupada = etapaRepository
                .existsByProcedimentoIdAndOrdem(procedimentoId, dto.ordem());
        if (ordemOcupada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma etapa com ordem %d neste procedimento.".formatted(dto.ordem()));
        }

        EtapaProcedimento etapa = new EtapaProcedimento();
        etapa.setProcedimento(procedimento);
        etapa.setOrdem(dto.ordem());
        etapa.setNome(dto.nome());
        etapa.setTipoEtapa(dto.tipoEtapa());
        etapa.setParametrosEtapa(dto.parametrosEtapa());
        etapa.setObrigatorio(dto.obrigatorio());

        return mapearEtapaParaDTO(etapaRepository.save(etapa));
    }

    @Transactional
    public EtapaProcedimentoResponseDTO atualizarEtapa(Long etapaId, EtapaProcedimentoUpdateDTO dto) {
        EtapaProcedimento etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa não encontrada."));

        // Se a ordem está mudando, verifica conflito
        if (dto.ordem() != null && !dto.ordem().equals(etapa.getOrdem())) {
            boolean ordemOcupada = etapaRepository
                    .existsByProcedimentoIdAndOrdem(etapa.getProcedimento().getId(), dto.ordem());
            if (ordemOcupada) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Já existe uma etapa com ordem %d neste procedimento.".formatted(dto.ordem()));
            }
            etapa.setOrdem(dto.ordem());
        }

        if (dto.nome() != null)           etapa.setNome(dto.nome());
        if (dto.tipoEtapa() != null)      etapa.setTipoEtapa(dto.tipoEtapa());
        if (dto.parametrosEtapa() != null) etapa.setParametrosEtapa(dto.parametrosEtapa());
        if (dto.obrigatorio() != null)    etapa.setObrigatorio(dto.obrigatorio());

        return mapearEtapaParaDTO(etapaRepository.save(etapa));
    }

    @Transactional
    public void deletarEtapa(Long etapaId) {
        if (!etapaRepository.existsById(etapaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa não encontrada.");
        }
        etapaRepository.deleteById(etapaId);
    }

    // [ ---------------- Mapeadores ---------------- ]

    private ProcedimentoResponseDTO mapearParaDTO(Procedimento procedimento) {

        List<EtapaProcedimentoResponseDTO> etapas = etapaRepository
                .findByProcedimentoIdOrderByOrdem(procedimento.getId())
                .stream()
                .map(this::mapearEtapaParaDTO)
                .toList();

        return new ProcedimentoResponseDTO(
                procedimento.getId(),
                procedimento.getSistema().getId(),
                procedimento.getSistema().getNome(),
                procedimento.getNome(),
                procedimento.getDescricao(),
                procedimento.getTipo(),
                procedimento.getConfigsGeral(),
                etapas
        );
    }

    private EtapaProcedimentoResponseDTO mapearEtapaParaDTO(EtapaProcedimento etapa) {
        return new EtapaProcedimentoResponseDTO(
                etapa.getIdEtapa(),
                etapa.getProcedimento().getId(),
                etapa.getOrdem(),
                etapa.getNome(),
                etapa.getTipoEtapa(),
                etapa.getParametrosEtapa(),
                etapa.getObrigatorio()
        );
    }
}