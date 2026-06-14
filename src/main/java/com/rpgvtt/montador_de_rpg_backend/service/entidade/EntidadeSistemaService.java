// EntidadeSistemaService.java
package com.rpgvtt.montador_de_rpg_backend.service.entidade;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeRelacao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeRelacaoKey;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.EntidadeProcedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.domain.validation.SchemaValidator;
import com.rpgvtt.montador_de_rpg_backend.dto.entidade.*;
import com.rpgvtt.montador_de_rpg_backend.dto.mecanica.*;
import com.rpgvtt.montador_de_rpg_backend.repository.mecanica.EntidadeProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeRelacaoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeSistemaRepository;
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
public class EntidadeSistemaService {

    private final EntidadeSistemaRepository entidadeRepository;
    private final EntidadeRelacaoRepository relacaoRepository;
    private final EntidadeProcedimentoRepository entidadeProcedimentoRepository;
    private final SistemaRepository sistemaRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final SchemaValidator schemaValidator;

    // [ ---------------- EntidadeSistema ---------------- ]

    @Transactional
    public EntidadeSistemaResponseDTO criar(EntidadeSistemaCreateDTO dto) {
        Sistema sistema = sistemaRepository.findById(dto.sistemaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sistema não encontrado."));

        EntidadeSistema entidade = new EntidadeSistema();
        entidade.setSistema(sistema);
        entidade.setTipo(dto.tipo());
        entidade.setNome(dto.nome());
        entidade.setDescricao(dto.descricao());
        entidade.setUrlImagem(dto.urlImagem());
        entidade.setAtributos(dto.atributos());
        entidade.setPropriedades(dto.propriedades());

        schemaValidator.validarEntidade(entidade, sistema);

        return mapearParaDTO(entidadeRepository.save(entidade));
    }

    @Transactional(readOnly = true)
    public EntidadeSistemaResponseDTO buscarPorId(Long id) {
        EntidadeSistema entidade = entidadeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade não encontrada."));
        return mapearParaDTO(entidade);
    }

    @Transactional(readOnly = true)
    public List<EntidadeSistemaResponseDTO> listarPorSistema(Long sistemaId) {
        if (!sistemaRepository.existsById(sistemaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sistema não encontrado.");
        }
        return entidadeRepository.findBySistemaId(sistemaId)
                .stream()
                .map(this::mapearParaDTO)
                .toList();
    }

    @Transactional
    public EntidadeSistemaResponseDTO atualizar(Long id, EntidadeSistemaUpdateDTO dto) {
        EntidadeSistema entidade = entidadeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade não encontrada."));

        if (dto.nome() != null)        entidade.setNome(dto.nome());
        if (dto.descricao() != null)   entidade.setDescricao(dto.descricao());
        if (dto.urlImagem() != null)   entidade.setUrlImagem(dto.urlImagem());
        if (dto.atributos() != null)   entidade.setAtributos(dto.atributos());
        if (dto.propriedades() != null) entidade.setPropriedades(dto.propriedades());

        schemaValidator.validarEntidade(entidade, entidade.getSistema());

        return mapearParaDTO(entidadeRepository.save(entidade));
    }

    @Transactional
    public void deletar(Long id) {
        if (!entidadeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade não encontrada.");
        }
        entidadeRepository.deleteById(id);
    }

    // [ ---------------- EntidadeRelacao ---------------- ]

//    @Transactional
//    public EntidadeRelacaoResponseDTO adicionarRelacao(EntidadeRelacaoCreateDTO dto) {
//
//        if (dto.idEntidadePai().equals(dto.idEntidadeFilha())) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma entidade não pode se relacionar consigo mesma.");
//        }
//
//        EntidadeSistema pai = entidadeRepository.findById(dto.idEntidadePai())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade pai não encontrada."));
//        EntidadeSistema filha = entidadeRepository.findById(dto.idEntidadeFilha())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade filha não encontrada."));
//
//        // Verifica se as duas entidades pertencem ao mesmo sistema
//        if (!pai.getSistema().getId().equals(filha.getSistema().getId())) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As entidades devem pertencer ao mesmo sistema.");
//        }
//
//        EntidadeRelacaoKey key = new EntidadeRelacaoKey(dto.idEntidadePai(), dto.idEntidadeFilha());
//
//        if (relacaoRepository.existsById(key)) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta relação já existe.");
//        }
//
//        EntidadeRelacao relacao = new EntidadeRelacao();
//        relacao.setId(key);
//        relacao.setIdEntidadePai(pai);
//        relacao.setIdEntidadeFilha(filha);
//        relacao.setQuantidade(dto.quantidade());
//        relacao.setCustomizacoes(dto.customizacoes());
//        relacao.setOrigem(dto.origem());
//
//        return mapearRelacaoParaDTO(relacaoRepository.save(relacao));
//    }
//
//    @Transactional
//    public EntidadeRelacaoResponseDTO atualizarRelacao(Long idPai, Long idFilha,
//                                                        EntidadeRelacaoUpdateDTO dto) {
//        EntidadeRelacaoKey key = new EntidadeRelacaoKey(idPai, idFilha);
//        EntidadeRelacao relacao = relacaoRepository.findById(key)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relação não encontrada."));
//
//        if (dto.quantidade() != null)    relacao.setQuantidade(dto.quantidade());
//        if (dto.customizacoes() != null) relacao.setCustomizacoes(dto.customizacoes());
//        if (dto.origem() != null)        relacao.setOrigem(dto.origem());
//
//        return mapearRelacaoParaDTO(relacaoRepository.save(relacao));
//    }
//
//    @Transactional
//    public void removerRelacao(Long idPai, Long idFilha) {
//        EntidadeRelacaoKey key = new EntidadeRelacaoKey(idPai, idFilha);
//        if (!relacaoRepository.existsById(key)) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Relação não encontrada.");
//        }
//        relacaoRepository.deleteById(key);
//    }
//
//    @Transactional(readOnly = true)
//    public List<EntidadeRelacaoResponseDTO> listarRelacoesDaEntidade(Long entidadeId) {
//        if (!entidadeRepository.existsById(entidadeId)) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade não encontrada.");
//        }
//        return relacaoRepository.findByIdEntidadePaiId(entidadeId)
//                .stream()
//                .map(this::mapearRelacaoParaDTO)
//                .toList();
//    }

    // [ ---------------- EntidadeProcedimento ---------------- ]

    @Transactional
    public EntidadeProcedimentoResponseDTO vincularProcedimento(EntidadeProcedimentoCreateDTO dto) {
        EntidadeSistema entidade = entidadeRepository.findById(dto.entidadeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade não encontrada."));
        Procedimento procedimento = procedimentoRepository.findById(dto.procedimentoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Procedimento não encontrado."));

        if (!entidade.getSistema().getId().equals(procedimento.getSistema().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A entidade e o procedimento devem pertencer ao mesmo sistema.");
        }

        EntidadeProcedimento ep = new EntidadeProcedimento();
        ep.setSistema(entidade.getSistema());
        ep.setEntidadeSistema(entidade);
        ep.setProcedimento(procedimento);
        ep.setProcessamento(dto.processamento());
        ep.setCondicao(dto.condicao());
        ep.setEReativo(dto.eReativo());
        ep.setOrdem(dto.ordem());

        return mapearEpParaDTO(entidadeProcedimentoRepository.save(ep));
    }

    @Transactional
    public EntidadeProcedimentoResponseDTO atualizarVinculo(Long id, EntidadeProcedimentoUpdateDTO dto) {
        EntidadeProcedimento ep = entidadeProcedimentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado."));

        if (dto.processamento() != null) ep.setProcessamento(dto.processamento());
        if (dto.condicao() != null)      ep.setCondicao(dto.condicao());
        if (dto.eReativo() != null)      ep.setEReativo(dto.eReativo());
        if (dto.ordem() != null)         ep.setOrdem(dto.ordem());

        return mapearEpParaDTO(entidadeProcedimentoRepository.save(ep));
    }

    @Transactional
    public void removerVinculo(Long id) {
        if (!entidadeProcedimentoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado.");
        }
        entidadeProcedimentoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<EntidadeProcedimentoResponseDTO> listarVinculosDaEntidade(Long entidadeId) {
        if (!entidadeRepository.existsById(entidadeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade não encontrada.");
        }
        return entidadeProcedimentoRepository.findByEntidadeSistemaId(entidadeId)
                .stream()
                .map(this::mapearEpParaDTO)
                .toList();
    }

    // [ ---------------- Mapeadores ---------------- ]

    private EntidadeSistemaResponseDTO mapearParaDTO(EntidadeSistema entidade) {
        return new EntidadeSistemaResponseDTO(
                entidade.getId(),
                entidade.getSistema().getId(),
                entidade.getSistema().getNome(),
                entidade.getTipo(),
                entidade.getNome(),
                entidade.getDescricao(),
                entidade.getUrlImagem(),
                entidade.getAtributos(),
                entidade.getPropriedades()
        );
    }

    private EntidadeRelacaoResponseDTO mapearRelacaoParaDTO(EntidadeRelacao relacao) {
        return new EntidadeRelacaoResponseDTO(
                relacao.getIdEntidadePai().getId(),
                relacao.getIdEntidadePai().getNome(),
                relacao.getIdEntidadeFilha().getId(),
                relacao.getIdEntidadeFilha().getNome(),
                relacao.getQuantidade(),
                relacao.getCustomizacoes(),
                relacao.getOrigem()
        );
    }

    private EntidadeProcedimentoResponseDTO mapearEpParaDTO(EntidadeProcedimento ep) {
        return new EntidadeProcedimentoResponseDTO(
                ep.getId(),
                ep.getSistema().getId(),
                ep.getEntidadeSistema().getId(),
                ep.getEntidadeSistema().getNome(),
                ep.getProcedimento().getId(),
                ep.getProcedimento().getNome(),
                ep.getProcessamento(),
                ep.getCondicao(),
                ep.isEReativo(),
                ep.getOrdem()
        );
    }
}