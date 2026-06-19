package com.rpgvtt.montador_de_rpg_backend.service.entidade;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeRelacao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import com.rpgvtt.montador_de_rpg_backend.domain.validation.SchemaValidator;
import com.rpgvtt.montador_de_rpg_backend.dto.entidade.EntidadeInstanciaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.entidade.EntidadeInstanciaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.entidade.EntidadeInstanciaUpadteDTO;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeRelacaoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeSistemaRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntidadeInstanciaService {

    private final EntidadeInstanciaRepository       instanciaRepository;
    private final EntidadeSistemaRepository         entidadeSistemaRepository;
    private final CampanhaRepository                campanhaRepository;
    private final EntidadeRelacaoRepository         entidadeRelacaoRepository;
    private final SchemaValidator                   schemaValidator;

    @Transactional
    public EntidadeInstanciaResponseDTO criar(EntidadeInstanciaCreateDTO dto) {
        Campanha campanha = campanhaRepository.findById(dto.campanhaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada."));

        EntidadeSistema entidadeSistema = entidadeSistemaRepository.findById(dto.entidadeSistemaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade do sistema não encontrada."));

        if (!entidadeSistema.getTipo().equals(dto.tipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O tipo informado não corresponde ao tipo da entidade do sistema.");
        }

        EntidadeInstancia instancia = new EntidadeInstancia();
        instancia.setCampanha(campanha);
        instancia.setEntidadeSistema(entidadeSistema);
        instancia.setTipo(dto.tipo());
        instancia.setNome(dto.nome());
        instancia.setDescricao(dto.descricao());
        instancia.setUrlImagem(dto.urlImagem());
        instancia.setAtributosAtuais(dto.atributosAtuais());
        instancia.setCustomizacoes(dto.customizacoes());

        schemaValidator.validarInstancia(instancia, entidadeSistema.getSistema());

        return mapearParaDTO(instanciaRepository.save(instancia));
    }

    @Transactional(readOnly = true)
    public EntidadeInstanciaResponseDTO buscarPorId(Long id) {
        EntidadeInstancia instancia = instanciaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instância não encontrada."));
        return mapearParaDTO(instancia);
    }

    @Transactional(readOnly = true)
    public List<EntidadeInstanciaResponseDTO> listarPorCampanha(Long campanhaId) {
        if (!campanhaRepository.existsById(campanhaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada.");
        }

        return instanciaRepository.findAll().stream()
                .filter(instancia -> instancia.getCampanha().getId().equals(campanhaId))
                .map(this::mapearParaDTO)
                .toList();
    }

    @Transactional
    public EntidadeInstanciaResponseDTO atualizar(Long id, EntidadeInstanciaUpadteDTO dto) {
        EntidadeInstancia instancia = instanciaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instância não encontrada."));

        if (dto.nome() != null) {
            instancia.setNome(dto.nome());
        }
        if (dto.descricao() != null) {
            instancia.setDescricao(dto.descricao());
        }
        if (dto.urlImagem() != null) {
            instancia.setUrlImagem(dto.urlImagem());
        }
        if (dto.tipo() != null && !dto.tipo().equals(instancia.getEntidadeSistema().getTipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível alterar o tipo para um valor diferente do tipo da entidade do sistema.");
        }
        if (dto.atributosAtuais() != null) {
            instancia.setAtributosAtuais(dto.atributosAtuais());
        }
        if (dto.customizacoes() != null) {
            instancia.setCustomizacoes(dto.customizacoes());
        }

        schemaValidator.validarInstancia(instancia, instancia.getEntidadeSistema().getSistema());

        return mapearParaDTO(instanciaRepository.save(instancia));
    }

    @Transactional
    public void deletar(Long id) {
        EntidadeInstancia instancia = instanciaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instância não encontrada."));

        List<EntidadeRelacao> relacoesPai = entidadeRelacaoRepository.findById_IdEntidadePai(id);
        if (!relacoesPai.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir a instância enquanto existirem relações dependentes.");
        }

        try {
            instanciaRepository.delete(instancia);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não foi possível excluir a instância devido a dependências de dados.");
        }
    }

    private EntidadeInstanciaResponseDTO mapearParaDTO(EntidadeInstancia instancia) {
        return new EntidadeInstanciaResponseDTO(
                instancia.getCampanha().getId(),
                instancia.getEntidadeSistema().getId(),
                instancia.getTipo(),
                instancia.getNome(),
                instancia.getDescricao(),
                instancia.getAtributosAtuais(),
                instancia.getCustomizacoes(),
                instancia.getCriadaEm(),
                instancia.getUrlImagem()
        );
    }
}
