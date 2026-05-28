package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import com.rpgvtt.montador_de_rpg_backend.repository.personagem.PersonagemRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.EtapaProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.ProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sistema.SistemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcedimentoEngine {

    private final ProcedimentoRepository procedimentoRepo;
    private final EtapaProcedimentoRepository etapaRepo;
    private final SistemaRepository sistemaRepo;
    private final PersonagemRepository personagemRepo;

    private Deque

}
