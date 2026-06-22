package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto;

import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.EscopoInstancias;
import com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.interfaces.ExecucaoContexto;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contexto mínimo para rodar UM handler fora de qualquer procedimento real —
 * usado por PrimitivoExecutor ao disparar gatilhos de item, efeitos manuais
 * do mestre, ou qualquer "primitivo avulso".
 *
 * Convenção: a "instância ativa" é sempre o EXECUTOR (quem realiza a ação).
 * O alvo nunca é inferido daqui — PrimitivoExecutor injeta "id_entidade"
 * explicitamente nos parâmetros antes de chamar o handler, seguindo o mesmo
 * padrão de override que AlterarAtributoHandler e CalcularAtributoHandler
 * já usam para procedimentos reais.
 */
public class ContextoAvulso implements ExecucaoContexto {

    private final Sistema sistema;
    private final ContextoAccessor contexto;
    private EscopoInstancias escopo;

    public ContextoAvulso(Sistema sistema, EntidadeInstancia executor, EntidadeInstancia alvo) {
        this.sistema = sistema;

        Map<String, Object> seed = new HashMap<>();
        seed.put("executor_id", executor.getId());
        seed.put("alvo_id", alvo.getId());
        this.contexto = new ContextoAccessor(seed);

        this.escopo = EscopoInstancias.unica(executor.getId());
    }

    @Override public ContextoAccessor getContexto()  { return contexto; }
    @Override public Sistema getSistema()           { return sistema; }
    @Override public Long getIdSistema()            { return sistema.getId(); }
    @Override public EscopoInstancias getEscopo()    { return escopo; }
    @Override public void setEscopo(EscopoInstancias escopo) {this.escopo = escopo; }
    @Override public boolean semInstancias()        { return false; }
    @Override public boolean temInstanciaUnica()    { return true; }
    @Override public Procedimento getProcedimento() { return null; }
    
    @Override
    public Long idInstanciaAtiva() {
        return ((EscopoInstancias.Unica) escopo).id();
    }

    @Override
    public List<Long> idsInstancias() {
        return List.of(idInstanciaAtiva());
    }
}
