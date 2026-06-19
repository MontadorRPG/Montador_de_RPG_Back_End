package com.rpgvtt.montador_de_rpg_backend.service.usuario;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusAmizade;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Amizade;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.AmizadeKey;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.dto.usuario.AmizadeResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.usuario.AmizadeSolicitarDTO;
import com.rpgvtt.montador_de_rpg_backend.repository.usuario.AmizadeRepository;
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
public class AmizadeService {

    private final AmizadeRepository amizadeRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public AmizadeResponseDTO enviarSolicitacao(AmizadeSolicitarDTO dto) {

        if (dto.remetenteId().equals(dto.destinatarioId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um usuário não pode se adicionar.");
        }

        
        amizadeRepository.findEntreUsuarios(dto.remetenteId(), dto.destinatarioId())
                .ifPresent(a -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma solicitação ou amizade entre estes usuários.");
                });

        Usuario remetente = usuarioRepository.findById(dto.remetenteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Remetente não encontrado."));
        Usuario destinatario = usuarioRepository.findById(dto.destinatarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinatário não encontrado."));

        AmizadeKey key = new AmizadeKey(dto.remetenteId(), dto.destinatarioId());

        Amizade amizade = new Amizade();
        amizade.setId(key);
        amizade.setRemetente(remetente);
        amizade.setDestinatario(destinatario);
        amizade.setStatus(StatusAmizade.PENDENTE);

        return mapearParaDTO(amizadeRepository.save(amizade));
    }

    @Transactional
    public AmizadeResponseDTO aceitarSolicitacao(Long remetenteId, Long destinatarioId) {

        AmizadeKey key = new AmizadeKey(remetenteId, destinatarioId);
        Amizade amizade = amizadeRepository.findById(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        if (amizade.getStatus() != StatusAmizade.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta solicitação já foi aceita.");
        }

        amizade.setStatus(StatusAmizade.ACEITO);
        amizade.setAceitoEm(LocalDateTime.now());

        return mapearParaDTO(amizadeRepository.save(amizade));
    }

    @Transactional
    public void remover(Long idUsuarioA, Long idUsuarioB) {
        Amizade amizade = amizadeRepository.findEntreUsuarios(idUsuarioA, idUsuarioB)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Amizade não encontrada."));

        amizadeRepository.delete(amizade);
    }

    @Transactional(readOnly = true)
    public List<AmizadeResponseDTO> listarAmigos(Long idUsuario) {
        return amizadeRepository.findByUsuarioAndStatus(idUsuario, StatusAmizade.ACEITO)
                .stream()
                .map(this::mapearParaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AmizadeResponseDTO> listarSolicitacoesPendentes(Long idUsuario) {
        return amizadeRepository.findById_IdDestinatarioAndStatus(idUsuario, StatusAmizade.PENDENTE)
                .stream()
                .map(this::mapearParaDTO)
                .toList();
    }

    private AmizadeResponseDTO mapearParaDTO(Amizade amizade) {
        return new AmizadeResponseDTO(
                amizade.getRemetente().getId(),
                amizade.getRemetente().getApelido(),
                amizade.getDestinatario().getId(),
                amizade.getDestinatario().getApelido(),
                amizade.getStatus().name(),
                amizade.getCriadaEm(),
                amizade.getAceitoEm()
        );
    }
}