package com.rpgvtt.montador_de_rpg_backend.engine.primitivos.handlers;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.EntityNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.exceptions.JsonFieldNotFoundException;
import com.rpgvtt.montador_de_rpg_backend.engine.primitivos.VerificadorAtributo;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.EtapaHandler;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.InstanciaResolver;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ProcedimentoContexto;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto.ResultadoEtapa;
import com.rpgvtt.montador_de_rpg_backend.repository.entidade.EntidadeInstanciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlterarAtributoHandler implements EtapaHandler {

    private final JsonMapper mapper;
    private final InstanciaResolver instanciaResolver;
    private final EntidadeInstanciaRepository instanciaRepo;
    private final VerificadorAtributo verificador;


    @Override
    public String tipoEtapa() {
        return "ALTERAR_ATRIBUTO";
    }

    @Override
    public ResultadoEtapa executar(EtapaProcedimento etapa, ProcedimentoContexto ctx) {

        Map<String, Object> params = mapper.convertValue(etapa.getParametros_etapa(), new TypeReference<>() {});

        String atributo = params.get("atributo").toString();

        Integer qtd;

        if (params.containsKey("source_key")){
            String sourceKey = params.get("source_key").toString();
            qtd = (Integer) ctx.getContexto().get(sourceKey);
        } else if (params.containsKey("quantidade") ){
            qtd = (Integer) params.get("quantidade");
        } else {
            throw new JsonFieldNotFoundException("quantidade", etapa.getNome());
        }

        Long idEntidade = (Long) params.get("id_entidade"); // opcional
        String op = params.getOrDefault("operacao", "").toString(); // soma, sub, div, mult (opcional)

        EntidadeInstancia inst;

        if (idEntidade == null) {
            inst = instanciaResolver.retornarAtiva(ctx);
        } else {
            inst = instanciaRepo.findById(idEntidade)
                    .orElseThrow(() -> new EntityNotFoundException(EntidadeInstancia.class, idEntidade));
        }

        Integer valAtributo = inst.getAtributosAtuais().get(atributo).asInt();

        double result = switch (op) {
            case "soma" -> valAtributo + qtd;
            case "sub" -> valAtributo - qtd;
            case "mult" -> valAtributo * qtd;
            case "div" -> (double) valAtributo / qtd;
            default -> qtd;
        };

        if (verificador.verificarAtributo(ctx.getIdSistema(), atributo, valAtributo)) {
            ObjectNode atributoFinal = inst.getAtributosAtuais().asObject().put(atributo, result);
            inst.setAtributosAtuais(atributoFinal);
            instanciaRepo.save(inst);
            return ResultadoEtapa.concluida(inst);
        }

        return null;
    }
}


