package com.rpgvtt.montador_de_rpg_backend.service;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.PapeisUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.CampanhaUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
import com.rpgvtt.montador_de_rpg_backend.service.exceptions.DeniedAcessException;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaUsuarioRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.personagem.PersonagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CampanhaAutorizacao {

    private final CampanhaUsuarioRepository campUsuarioRepo;
    private final PersonagemRepository personagemRepo;

    public CampanhaUsuario exigirMestre(Long idCampanha, Long idUsuario) {
        return campUsuarioRepo
                .findByUsuarioIdAndCampanhaIdAndPapel(idCampanha, idUsuario, PapeisUsuario.MESTRE)
                .orElseThrow(() -> new DeniedAcessException(
                        "Usuário " + idUsuario + " não é mestre da campanha " + idCampanha));
    }

    public CampanhaUsuario exigirMembro(Long idCampanha, Long idUsuario) {
        return campUsuarioRepo
                .findByCampanhaIdAndUsuarioId(idCampanha, idUsuario)
                .orElseThrow(() -> new DeniedAcessException(
                        "Usuário " + idUsuario + " não é membro da campanha " + idCampanha));
    }

    public boolean isMestre(Long idCampanha, Long idUsuario) {
        return campUsuarioRepo
                .findByCampanhaIdAndUsuarioId(idCampanha, idUsuario)
                .map(cu -> cu.getPapel() == PapeisUsuario.MESTRE)
                .orElse(false);
    }

    /**
     * A player can auto-join if they have a personagem in the campaign
     * that is alive (hp > 0) and not marked as inactive.
     */
    public Personagem exigirPersonagemVivo(Long idCampanha, Long idUsuario) {
        return personagemRepo
                .findAtivoByCampanhaIdAndUsuarioId(idCampanha, idUsuario)
                .orElseThrow(() -> new DeniedAcessException(
                        "Nenhum personagem vivo nesta campanha para o usuário " + idUsuario));
    }
}