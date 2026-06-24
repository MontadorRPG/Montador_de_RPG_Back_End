package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.EntidadeProcedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.*;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.*;
import com.rpgvtt.montador_de_rpg_backend.repository.mecanica.EntidadeProcedimentoRepository;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UsarHabilidadeHandler implements EtapaHandler {

    private final EntidadeInstanciaRepository instanciaRepo;
    private final EntidadeProcedimentoRepository entProcRepo;
    private final ChamarProcedimentoHandler chamarProcHandler;
    private final VerificarCondicaoHandler verificarCondicao;
    private final ObjectMapper mapper;

    @Override
    public String tipoEtapa() { return "USAR_HABILIDADE"; }

    @Override
    public ResultadoEtapa executar(EtapaExecutavel etapa, ExecucaoContexto ctx) {
        JsonNode params = etapa.getParametrosEtapa();
        Long idInstancia = params.get("id_instancia").asLong();

        EntidadeInstancia instanciaHabilidade = instanciaRepo.findById(idInstancia)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Instância de habilidade não encontrada: " + idInstancia));

        // Busca EntidadeProcedimento vinculada à instância
        EntidadeProcedimento entProc = entProcRepo
                .findByEntidadeInstancia(instanciaHabilidade)
                .orElse(null);

        if (entProc == null) {
            return ResultadoEtapa.erro("Nenhum procedimento definido para esta habilidade");
        }

        JsonNode condicao = entProc.getCondicao();
        boolean temCondicao = condicao != null && !condicao.isNull() && !condicao.isEmpty();
        if (temCondicao) {
            ResultadoEtapa resultadoCondicao = verificarCondicao.verificar(condicao, ctx);
            
            // Se não for CONCLUIDA, propaga erro
            if (resultadoCondicao.tipo() != ResultadoEtapa.Tipo.CONCLUIDA) {
                return resultadoCondicao;
            }
            
            // Verifica se a condição foi satisfeita
            @SuppressWarnings("unchecked")
            Map<String, Object> dados = (Map<String, Object>) resultadoCondicao.dados();
            String status = dados != null ? (String) dados.get("status") : null;
            if (!"condicao_ok".equals(status) && !"sem_condicao".equals(status)) {
                return resultadoCondicao; // condição falhou
            }
        }

        // Monta parâmetros para o sub-procedimento
        Map<String, Object> paramsChamada = Map.of(
                "id_procedimento", entProc.getProcedimento().getId(),
                "salvar_em", "resultado_habilidade",
                "escopo", "HERDAR",
                "passar_contexto", List.of("executor_id", "alvo_id")
        );

        // Cria uma etapa do tipo CHAMAR_PROCEDIMENTO
        EtapaProcedimento etapaChamada = new EtapaProcedimento();
        etapaChamada.setParametrosEtapa(mapper.valueToTree(paramsChamada));

        // Chama o handler (certifique-se de que o ctx é um ProcedimentoContexto)
        if (!(ctx instanceof ProcedimentoContexto procedimentoCtx)) {
            return ResultadoEtapa.erro("USAR_HABILIDADE requer um procedimento ativo");
        }

        return chamarProcHandler.executar(etapaChamada, procedimentoCtx);
    }
}