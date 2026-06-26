package com.rpgvtt.montador_de_rpg_backend.controller.DadosFront;

import java.time.LocalDate;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.rpgvtt.montador_de_rpg_backend.domain.enums.PapeisUsuario;
import com.rpgvtt.montador_de_rpg_backend.domain.enums.StatusCampanha;
import com.rpgvtt.montador_de_rpg_backend.repository.campanha.CampanhaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.CenaRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.sessao.SessaoRepository;
import com.rpgvtt.montador_de_rpg_backend.security.UsuarioPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final CampanhaRepository campanhaRepo;
    private final CenaRepository cenaRepo;
    private final SessaoRepository sessaoRepo;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getIndicadores() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long idUsuario = ((UsuarioPrincipal) auth.getPrincipal()).getId();

        long campanhasAtivas = campanhaRepo.countByUsuarioIdAndStatus(idUsuario, StatusCampanha.ATIVA);
        LocalDate hoje = LocalDate.now();
        long sessoesEsteMes = sessaoRepo.countByUsuarioIdAndMesAtual(idUsuario, hoje.getMonthValue(), hoje.getYear());
        long jogadores = campanhaRepo.countJogadoresNasMesmasCampanhas(idUsuario);

        long cenasCriadas = cenaRepo.countByCriadorId(idUsuario, PapeisUsuario.MESTRE);
        long cenasParaPreparar = cenaRepo.countByCriadorIdAndCamposNulos(idUsuario, PapeisUsuario.MESTRE);

        Map<String, Object> indicadores = new LinkedHashMap<>();
        indicadores.put("campanhasAtivas", campanhasAtivas);
        indicadores.put("jogadores", jogadores);
        indicadores.put("cenasCriadas", cenasCriadas);
        indicadores.put("sessoesEsteMes", sessoesEsteMes);
        indicadores.put("cenasParaPreparar", cenasParaPreparar);

        return ResponseEntity.ok(indicadores);
    }
}