package com.rpgvtt.montador_de_rpg_backend.service.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;

public record ParticipanteSessao(
        Long idUsuario,
        boolean mestre,
        EntidadeInstancia instancia // null for master
) {}
