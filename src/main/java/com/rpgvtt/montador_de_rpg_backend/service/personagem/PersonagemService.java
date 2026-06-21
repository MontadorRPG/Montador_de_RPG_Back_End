package com.rpgvtt.montador_de_rpg_backend.service.personagem;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import com.rpgvtt.montador_de_rpg_backend.domain.validation.SchemaValidator;
import com.rpgvtt.montador_de_rpg_backend.dto.personagem.PersonagemCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.personagem.PersonagemCompletoCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.personagem.PersonagemResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.personagem.PersonagemUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeSistemaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.personagem.PersonagemRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonagemService {

    private final PersonagemRepository personagemRepository;
    private final UsuarioRepository usuarioRepository;
    private final CampanhaRepository campanhaRepository;
    private final EntidadeInstanciaRepository entidadeInstanciaRepository;
    private final EntidadeSistemaRepository entidadeSistemaRepository;
    private final SchemaValidator schemaValidator;

    @Transactional
    public PersonagemResponseDTO criar(PersonagemCreateDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        Campanha campanha = null;
        if (dto.campanhaId() != null) {
            campanha = campanhaRepository.findById(dto.campanhaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada."));
        }

        EntidadeInstancia instancia = entidadeInstanciaRepository.findById(dto.instanciaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instância não encontrada."));

        // Validar que a instância pertence à campanha
        if (!instancia.getCampanha().getId().equals(dto.campanhaId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A instância não pertence à campanha informada.");
        }

        Personagem personagem = new Personagem();
        personagem.setUsuario(usuario);
        personagem.setCampanha(campanha);
        personagem.setInstancia(instancia);
        personagem.setHistoria(dto.historia());
        personagem.setAparencia(dto.aparencia());
        personagem.setUrlImagem(dto.urlImagem());
        personagem.setNotasJogador(dto.notasJogador());
        personagem.setAtivo(true); // Novo personagem inicia como ativo

        return mapearParaDTO(personagemRepository.save(personagem));
    }

    @Transactional
    public PersonagemResponseDTO criarCompleto(PersonagemCompletoCreateDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        Campanha campanha = dto.campanhaId() != null
        ? campanhaRepository.findById(dto.campanhaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada."))
        : null;

        EntidadeSistema entidadeSistema = entidadeSistemaRepository.findById(dto.entidadeSistemaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade do sistema não encontrada."));

        if (!entidadeSistema.getTipo().equals(dto.tipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O tipo informado não corresponde ao tipo da entidade do sistema.");
        }

        // 1. Criar a EntidadeInstancia
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
        EntidadeInstancia instanciaSalva = entidadeInstanciaRepository.save(instancia);

        // 2. Criar o Personagem vinculado à instância criada
        Personagem personagem = new Personagem();
        personagem.setUsuario(usuario);
        personagem.setCampanha(campanha);
        personagem.setInstancia(instanciaSalva);
        personagem.setHistoria(dto.historia());
        personagem.setAparencia(dto.aparencia());
        personagem.setUrlImagem(dto.urlImagem());
        personagem.setNotasJogador(dto.notasJogador());
        personagem.setAtivo(true);

        return mapearParaDTO(personagemRepository.save(personagem));
    }

    @Transactional(readOnly = true)
    public PersonagemResponseDTO buscarPorId(Long id) {
        Personagem personagem = personagemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personagem não encontrado."));
        return mapearParaDTO(personagem);
    }

    @Transactional(readOnly = true)
    public List<PersonagemResponseDTO> listarPorCampanha(Long campanhaId) {
        if (!campanhaRepository.existsById(campanhaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada.");
        }

        return personagemRepository.findAll().stream()
                .filter(p -> p.getCampanha() != null && p.getCampanha().getId().equals(campanhaId))
                .map(this::mapearParaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PersonagemResponseDTO> listarPorUsuario(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
        }

        return personagemRepository.findAll().stream()
                .filter(personagem -> personagem.getUsuario().getId().equals(usuarioId))
                .map(this::mapearParaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonagemResponseDTO buscarAtivoDoJogador(Long campanhaId, Long usuarioId) {
        Personagem personagem = personagemRepository.findAtivoByCampanhaIdAndUsuarioId(campanhaId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhum personagem ativo encontrado para este jogador nesta campanha."));
        return mapearParaDTO(personagem);
    }

    @Transactional
    public PersonagemResponseDTO atualizar(Long id, PersonagemUpdateDTO dto) {
        Personagem personagem = personagemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personagem não encontrado."));

        if (dto.historia() != null) {
            personagem.setHistoria(dto.historia());
        }
        if (dto.aparencia() != null) {
            personagem.setAparencia(dto.aparencia());
        }
        if (dto.urlImagem() != null) {
            personagem.setUrlImagem(dto.urlImagem());
        }
        if (dto.notasJogador() != null) {
            personagem.setNotasJogador(dto.notasJogador());
        }
        personagem.setAtivo(dto.ativo());

        return mapearParaDTO(personagemRepository.save(personagem));
    }

    @Transactional
    public void deletar(Long id) {
        if (!personagemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Personagem não encontrado.");
        }
        personagemRepository.deleteById(id);
    }

    private PersonagemResponseDTO mapearParaDTO(Personagem personagem) {
    Long campanhaId = personagem.getCampanha() != null ? personagem.getCampanha().getId() : null;
    String campanhaNome = personagem.getCampanha() != null ? personagem.getCampanha().getNome() : null;

        return new PersonagemResponseDTO(
            personagem.getId(),
            personagem.getUsuario().getId(),
            personagem.getUsuario().getEmail(),
            campanhaId,
            campanhaNome,
            personagem.getInstancia().getId(),
            personagem.getInstancia().getNome(),
            personagem.getHistoria(),
            personagem.getAparencia(),
            personagem.getUrlImagem(),
            personagem.getNotasJogador(),
            personagem.isAtivo(),
            personagem.getCriadoEm()
        );
    }
}