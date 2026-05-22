package com.rpgvtt.montador_de_rpg_backend.service.campanha;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.PapeisUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuarioKey;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.AdicionarJogadorDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaCreateDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaParticipanteResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.campanha.CampanhaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.repository.CampanhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.CampanhaUsuarioRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.UsuarioRepository; // IMPORT ADICIONADO
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CampanhaService {

    private final CampanhaRepository campanhaRepository;
    private final CampanhaUsuarioRepository campanhaUsuarioRepository;
    private final UsuarioRepository usuarioRepository; // ATRIBUTO ADICIONADO
    private final EntityManager entityManager;

    // CONSTRUTOR ATUALIZADO COM A INJEÇÃO DO USUARIO_REPOSITORY
    public CampanhaService(CampanhaRepository campanhaRepository, 
                           CampanhaUsuarioRepository campanhaUsuarioRepository, 
                           UsuarioRepository usuarioRepository, 
                           EntityManager entityManager) {
        this.campanhaRepository = campanhaRepository;
        this.campanhaUsuarioRepository = campanhaUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public CampanhaResponseDTO criar(CampanhaCreateDTO dto) {
        Campanha campanha = new Campanha();
        campanha.setNome(dto.nome());
        
        Sistema sistemaProxy = entityManager.getReference(Sistema.class, dto.sistemaId());
        campanha.setSistema(sistemaProxy);

        campanha = campanhaRepository.save(campanha);

        Usuario usuarioProxy = entityManager.getReference(Usuario.class, dto.criadorId());
        
        CampanhaUsuarioKey vinculoKey = new CampanhaUsuarioKey(campanha.getId(), dto.criadorId());
        CampanhaUsuario vinculo = new CampanhaUsuario();
        vinculo.setId(vinculoKey);
        vinculo.setCampanha(campanha);
        vinculo.setUsuario(usuarioProxy);
        vinculo.setPapel(PapeisUsuario.MESTRE); 

        // Alterado de entityManager.persist para usar o Repository por consistência
        campanhaUsuarioRepository.save(vinculo);

        return mapToResponseDTO(campanha);
    }

    @Transactional(readOnly = true)
    public CampanhaResponseDTO buscarPorId(Long id) {
        Campanha campanha = campanhaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada"));
        return mapToResponseDTO(campanha);
    }

    @Transactional(readOnly = true)
    public List<CampanhaResponseDTO> listarTodas() {
        return campanhaRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional
    public void deletar(Long id) {
        if (!campanhaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada");
        }
        campanhaRepository.deleteById(id);
    }

    @Transactional
    public CampanhaParticipanteResponseDTO adicionarJogador(Long campanhaId, AdicionarJogadorDTO dto) {
        if (!campanhaRepository.existsById(campanhaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada");
        }

        // Agora compila perfeitamente porque o repositório foi injetado!
        if (!usuarioRepository.existsById(dto.usuarioId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }

        CampanhaUsuarioKey key = new CampanhaUsuarioKey(campanhaId, dto.usuarioId());

        if (campanhaUsuarioRepository.existsById(key)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário já está participando desta campanha");
        }

        Campanha campanhaProxy = entityManager.getReference(Campanha.class, campanhaId);
        Usuario usuarioProxy = entityManager.getReference(Usuario.class, dto.usuarioId());

        CampanhaUsuario novoVinculo = new CampanhaUsuario();
        novoVinculo.setId(key);
        novoVinculo.setCampanha(campanhaProxy);
        novoVinculo.setUsuario(usuarioProxy);
        novoVinculo.setPapel(PapeisUsuario.JOGADOR); // Limpo o pacote redundante aqui

        novoVinculo = campanhaUsuarioRepository.save(novoVinculo);

        return new CampanhaParticipanteResponseDTO(
                novoVinculo.getId().getIdCampanha(),
                novoVinculo.getId().getIdusuario(), // Atenção apenas se o 'u' maiúsculo não quebrar aqui
                novoVinculo.getPapel().name(),
                novoVinculo.getEntrouEm()
        );
    }

    private CampanhaResponseDTO mapToResponseDTO(Campanha campanha) {
        return new CampanhaResponseDTO(
                campanha.getId(),
                campanha.getNome(),
                campanha.getCriadaEm(),
                campanha.getSistema().getId(),
                campanha.getSistema().getNome()
        );
    }
}