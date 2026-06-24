package com.rpgvtt.montador_de_rpg_backend.controller.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Cena;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.AtributoAlteradoDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.CenaResponseDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.ConviteDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.EntradaSessaoDTO;
import com.rpgvtt.montador_de_rpg_backend.dto.sessao.SessaoDTO;
import com.rpgvtt.montador_de_rpg_backend.security.UsuarioPrincipal;
import com.rpgvtt.montador_de_rpg_backend.service.sessao.SessaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessoes")
@RequiredArgsConstructor
public class SessaoController {

    private final SessaoService sessaoService;

    /**
     * POST /api/sessoes/campanhas/{idCampanha}/iniciar
     * Master starts a session.
     */
    @PostMapping("/campanhas/{idCampanha}/iniciar")
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoDTO iniciar(@PathVariable Long idCampanha,
                             @AuthenticationPrincipal UsuarioPrincipal principal) {
        return sessaoService.iniciarSessao(idCampanha, principal.getId());
    }

    /**
     * POST /api/sessoes/{idSessao}/encerrar
     * Master ends the session.
     */
    @PostMapping("/{idSessao}/encerrar")
    public void encerrar(@PathVariable Long idSessao,
                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        sessaoService.encerrarSessao(idSessao, principal.getId());
    }

    /**
     * POST /api/sessoes/{idSessao}/entrar
     * Player (or master) joins the session room.
     * Optional body: { "tokenConvite": "..." }
     */
    @PostMapping("/{idSessao}/entrar")
    public EntradaSessaoDTO entrar(@PathVariable Long idSessao,
                                   @RequestBody(required = false) EntrarRequest req,
                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        String token = req != null ? req.tokenConvite() : null;
        return sessaoService.entrarNaSessao(idSessao, principal.getId(), token);
    }

    /**
     * POST /api/sessoes/{idSessao}/convite
     * Master invites a specific user. Body: { "idUsuarioAlvo": 42 }
     */
    @PostMapping("/{idSessao}/convite")
    public ConviteDTO convidar(@PathVariable Long idSessao,
                               @RequestBody ConviteRequest req,
                               @AuthenticationPrincipal UsuarioPrincipal principal) {
        return sessaoService.gerarConvite(
                idSessao, req.idUsuarioAlvo(), principal.getId());
    }

    /**
     * PATCH /api/sessoes/{idSessao}/instancias/{idInstancia}/atributos
     * Master changes any attribute on any instance.
     * Body: { "atributo": "hp", "valor": 25 }
     */
    @PatchMapping("/{idSessao}/instancias/{idInstancia}/atributos")
    public AtributoAlteradoDTO alterarAtributoMestre(
            @PathVariable Long idSessao,
            @PathVariable Long idInstancia,
            @RequestBody AlterarAtributoRequest req,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return sessaoService.alterarAtributoInstancia(
                idSessao, idInstancia,
                req.atributo(), req.valor(),
                principal.getId()
        );
    }

    /**
     * PATCH /api/sessoes/{idSessao}/meu-personagem/atributos
     * Player changes their own non-combat attributes.
     */
    @PatchMapping("/{idSessao}/meu-personagem/atributos")
    public AtributoAlteradoDTO alterarAtributoJogador(
            @PathVariable Long idSessao,
            @RequestBody AlterarAtributoRequest req,
            @AuthenticationPrincipal UsuarioPrincipal principal) {

        // Resolve idInstancia from the player's active character
        Long idInstancia = sessaoService.resolverInstanciaDoJogador(
                idSessao, principal.getId());

        return sessaoService.alterarAtributoPersonagem(
                idSessao, idInstancia,
                req.atributo(), req.valor(),
                principal.getId()
        );
    }

    // Request/response records
    public record EntrarRequest(String tokenConvite) {}
    public record ConviteRequest(Long idUsuarioAlvo) {}
    public record AlterarAtributoRequest(String atributo, Object valor) {}
}