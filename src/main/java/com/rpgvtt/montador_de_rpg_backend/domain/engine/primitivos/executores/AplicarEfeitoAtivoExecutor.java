package com.rpgvtt.montador_de_rpg_backend.domain.engine.primitivos.executores;

import com.rpgvtt.montador_de_rpg_backend.domain.engine.components.InterpretadorJson;
import com.rpgvtt.montador_de_rpg_backend.domain.engine.primitivos.EstadoSessao;
import com.rpgvtt.montador_de_rpg_backend.domain.engine.primitivos.PrimitivoExecutor;
import com.rpgvtt.montador_de_rpg_backend.domain.engine.utils.Contexto;
import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.EntidadeEfeito;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.EfeitoAtivo;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeEfeitoRepository;
import tools.jackson.databind.JsonNode;

// Primitivo: instancia um EfeitoAtivo na sessão
// Parâmetros esperados:
//   "id_efeito"      → id do EntidadeEfeito a instanciar
//   "id_alvo"        → expressão que resolve para o id da entidade alvo
//   "usos"           → opcional — número de usos
//   "expira_em"      → número de rodadas/turnos até expirar
public class AplicarEfeitoAtivoExecutor implements PrimitivoExecutor {

    private final InterpretadorJson interpretador;
    private final EntidadeEfeitoRepository efeitoRepository;

    public AplicarEfeitoAtivoExecutor(
            InterpretadorJson interpretador,
            EntidadeEfeitoRepository efeitoRepository) {
        this.interpretador = interpretador;
        this.efeitoRepository = efeitoRepository;
    }

    @Override
    public void executar(JsonNode parametros, Contexto contexto, EstadoSessao estado) {

        JsonNode idEfeitoNode = parametros.get("id_efeito");
        JsonNode idAlvoNode   = parametros.get("id_alvo");
        JsonNode expiraEmNode = parametros.get("expira_em");

        if (idEfeitoNode == null || idAlvoNode == null || expiraEmNode == null) {
            throw new IllegalArgumentException(
                "aplicar_efeito_ativo requer 'id_efeito', 'id_alvo' e 'expira_em'"
            );
        }

        long idEfeito = (long) interpretador.interpretar(idEfeitoNode, contexto).comoNumero();
        long idAlvo   = (long) interpretador.interpretar(idAlvoNode, contexto).comoNumero();
        int expiraEm  = (int) interpretador.interpretar(expiraEmNode, contexto).comoNumero();

        EntidadeEfeito efeito = efeitoRepository.findById(idEfeito)
            .orElseThrow(() -> new IllegalArgumentException(
                "EntidadeEfeito %d não encontrado".formatted(idEfeito)
            ));

        EfeitoAtivo efeitoAtivo = new EfeitoAtivo();
        efeitoAtivo.setEntidadeEfeito(efeito);
        efeitoAtivo.setSessao(estado.getSessao());
        efeitoAtivo.setEntidadeInstancia(estado.getEntidades().get(idAlvo));
        efeitoAtivo.setExpiraEm(expiraEm);

        if (parametros.has("usos")) {
            int usos = (int) interpretador.interpretar(parametros.get("usos"), contexto).comoNumero();
            efeitoAtivo.setUsosRestantes(usos);
        }

        estado.adicionarEfeito(efeitoAtivo);
    }
}