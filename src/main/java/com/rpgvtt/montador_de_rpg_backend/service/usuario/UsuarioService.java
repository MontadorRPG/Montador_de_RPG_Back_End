package com.rpgvtt.montador_de_rpg_backend.service.usuario;

import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.dto.usuario.UsuarioResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.usuario.UsuarioUpdateDTO;
import com.rpgvtt.montador_de_rpg_backend.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return mapearParaDTO(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorEmail(String email) {
        // Útil para o frontend buscar o próprio perfil após o login OAuth2,
        // já que o JWT carrega o email, não o ID
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return mapearParaDTO(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        
        return usuarioRepository.findAll().stream()
                .map(this::mapearParaDTO)
                .toList();
    }

    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        usuario.setApelido(dto.apelido());
        usuario.setUrlImagem(dto.urlImagem());
        
        return mapearParaDTO(usuario);
    }

    @Transactional
    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
        }
        usuarioRepository.deleteById(id);
    }

    private UsuarioResponseDTO mapearParaDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getApelido(),
                usuario.getUrlImagem(),
                usuario.isE_admin(),
                usuario.getCriadoEm()
        );
    }
}