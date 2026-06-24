// package com.rpgvtt.montador_de_rpg_backend.config;

// import com.rpgvtt.montador_de_rpg_backend.domain.enums.BatalhaStatus;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.batalha.Batalha;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.batalha.BatalhaParticipantes;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.Primitivo;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EtapaProcedimento;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.EventoSistema;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Procedimento;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
// import tools.jackson.databind.JsonNode;
// import tools.jackson.databind.json.JsonMapper;

// import java.util.Map;

// /**
//  * Static factory helpers that produce fully-wired, persisted domain objects.
//  *
//  * Keeping fixtures here prevents test methods from drowning in setup boilerplate.
//  * Each builder method saves to DB via EntityManager and returns the persisted entity.
//  */
// public final class TestFixtures {

//     private static final JsonMapper OM = new JsonMapper();

//     // ── Sistema ───────────────────────────────────────────────────

//     public static Sistema sistemaDnd5e(jakarta.persistence.EntityManager em) {
//         Sistema s = new Sistema();
//         s.setNome("D&D 5e Test");
//         s.setVersaoSchemas(1);
//         s.setEOficial(false);
//         s.setConfiguracao(jsonb(Map.of(
//                 "formula_modificador",  "DND5E",
//                 "duracao_rodada_segundos", 6,
//                 "acoes_disponiveis",    java.util.List.of("ATACAR", "LANÇAR_MAGIA", "DASH"),
//                 "acoes_bonus_disponiveis", java.util.List.of("ATACAR_BONUS")
//         )));
//         em.persist(s);
//         return s;
//     }

//     // ── EntidadeSistema ───────────────────────────────────────────

//     public static EntidadeSistema entidadePersonagem(Sistema sistema,
//                                                      jakarta.persistence.EntityManager em) {
//         EntidadeSistema e = new EntidadeSistema();
//         e.setSistema(sistema);
//         e.setTipo("PERSONAGEM");
//         e.setNome("Personagem");
//         e.setAtributos(jsonb(Map.of(
//                 "hp",         Map.of("tipo", "numero"),
//                 "hp_max",     Map.of("tipo", "numero"),
//                 "forca",      Map.of("tipo", "numero"),
//                 "ca",         Map.of("tipo", "numero"),
//                 "velocidade", Map.of("tipo", "numero")
//         )));
//         em.persist(e);
//         return e;
//     }

//     // ── EntidadeInstancia ─────────────────────────────────────────

//     public static EntidadeInstancia instanciaGuerreiro(Campanha campanha,
//                                                        EntidadeSistema entidade,
//                                                        jakarta.persistence.EntityManager em) {
//         EntidadeInstancia inst = new EntidadeInstancia();
//         inst.setCampanha(campanha);
//         inst.setEntidadeSistema(entidade);
//         inst.setTipo("PERSONAGEM");
//         inst.setNome("Thorin");
//         inst.setAtributosAtuais(jsonb(Map.of(
//                 "hp",          30,
//                 "hp_max",      30,
//                 "forca",       18,   // +4 modifier
//                 "ca",          16,
//                 "velocidade",  30,
//                 "acao_bonus_disponivel", true
//         )));
//         em.persist(inst);
//         return inst;
//     }

//     public static EntidadeInstancia instanciaGoblin(Campanha campanha,
//                                                     EntidadeSistema entidade,
//                                                     jakarta.persistence.EntityManager em) {
//         EntidadeInstancia inst = new EntidadeInstancia();
//         inst.setCampanha(campanha);
//         inst.setEntidadeSistema(entidade);
//         inst.setTipo("INIMIGO");
//         inst.setNome("Goblin");
//         inst.setAtributosAtuais(jsonb(Map.of(
//                 "hp",     7,
//                 "hp_max", 7,
//                 "forca",  8,    // -1 modifier
//                 "ca",     15,
//                 "velocidade", 30
//         )));
//         em.persist(inst);
//         return inst;
//     }

//     // ── Rolagem ───────────────────────────────────────────────────

// //    public static Rolagem rolagem(String dado, int quantidade, boolean explosao,
// //                                  jakarta.persistence.EntityManager em) {
// //        Rolagem r = new Rolagem();
// //        r.setDado(dado);
// //        r.setQuantidade(quantidade);
// //        r.setExplosao(explosao);
// //        em.persist(r);
// //        return r;
// //    }

//     // ── Procedimento + Etapas ─────────────────────────────────────

//     /**
//      * Builds the minimal TURNO_COMBATE procedure:
//      *   ordem 1 — APLICAR_EFEITOS_ATIVOS (INICIO_TURNO)
//      *   ordem 2 — SOLICITAR_INPUT        (acao principal)
//      *   ordem 3 — DISPARAR_EVENTO        (fires chosen action)
//      *   ordem 4 — APLICAR_EFEITOS_ATIVOS (FIM_TURNO)
//      */
//     public static Procedimento procedimentoTurnoCombate(Sistema sistema,
//                                                         jakarta.persistence.EntityManager em) {
//         Procedimento p = new Procedimento();
//         p.setSistema(sistema);
//         p.setNome("Turno de Combate Test");
//         p.setTipo("TURNO_COMBATE");
//         p.setConfigsGeral(jsonb(Map.of()));
//         em.persist(p);

//         etapa(p, 1, "Início turno", "APLICAR_EFEITOS_ATIVOS",
//                 Map.of("momento", "INICIO_TURNO", "alvo", "instancia_ativa"), true, em);

//         etapa(p, 2, "Ação principal", "SOLICITAR_INPUT",
//                 Map.of("slot", "ACAO",
//                         "opcoes_fonte", "configs_geral.acoes_disponiveis",
//                         "pode_passar", true,
//                         "salvar_em", "acao_escolhida"), false, em);

//         etapa(p, 3, "Executar ação", "DISPARAR_EVENTO",
//                 Map.of("evento_fonte",   "contexto",
//                         "chave_contexto", "acao_escolhida",
//                         "alvo_fonte",     "input",
//                         "alvo_chave",     "id_alvo"), false, em);

//         etapa(p, 4, "Fim turno", "APLICAR_EFEITOS_ATIVOS",
//                 Map.of("momento", "FIM_TURNO", "alvo", "instancia_ativa"), true, em);

//         return p;
//     }

//     public static EtapaProcedimento etapa(Procedimento proc, int ordem, String nome,
//                                           String tipo, Map<String, Object> params,
//                                           boolean obrigatorio,
//                                           jakarta.persistence.EntityManager em) {
//         EtapaProcedimento e = new EtapaProcedimento();
//         e.setProcedimento(proc);
//         e.setOrdem(ordem);
//         e.setNome(nome);
//         e.setTipoEtapa(tipo);
//         e.setParametrosEtapa(jsonb(params));
//         e.setObrigatorio(obrigatorio);
//         em.persist(e);
//         return e;
//     }

//     // ── EventoSistema ─────────────────────────────────────────────

//     public static EventoSistema eventoAtaque(Sistema sistema,
//                                              jakarta.persistence.EntityManager em) {
//         EventoSistema ev = new EventoSistema();
//         ev.setSistema(sistema);
//         ev.setNome("ATACAR");
//         ev.setDescricao("Ataque corpo a corpo");
//         ev.setPayloadSchema(jsonb(Map.of("id_alvo", "Long")));
//         em.persist(ev);
//         return ev;
//     }

//     // ── Primitivo ─────────────────────────────────────────────────

//     public static Primitivo primitivoDano(jakarta.persistence.EntityManager em) {
//         Primitivo p = new Primitivo();
//         p.setNome("DANO");
//         p.setDescricao("Reduz HP do alvo");
//         p.setParametroSchemas(jsonb(Map.of(
//                 "valor", "int",
//                 "tipo",  "string"
//         )));
//         em.persist(p);
//         return p;
//     }

//     // ── Batalha ───────────────────────────────────────────────────

//     public static Batalha batalha(Sessao sessao, jakarta.persistence.EntityManager em) {
//         Batalha b = new Batalha();
//         b.setSessao(sessao);
//         b.setStatus(BatalhaStatus.EM_ANDAMENTO);
//         b.setRodadaAtual(1);
//         em.persist(b);
//         return b;
//     }

//     public static BatalhaParticipantes addParticipante(Batalha batalha,
//                                                        EntidadeInstancia inst,
//                                                        int lado, int ordemIniciativa,
//                                                        jakarta.persistence.EntityManager em) {
//         BatalhaParticipantes bi = new BatalhaParticipantes();
//         bi.setBatalha(batalha);
//         bi.setEntidadeInstancia(inst);
//         bi.setLado(lado);
//         bi.setOrdemIniciativa(ordemIniciativa);
//         bi.setAtivo(true);
//         em.persist(bi);
//         return bi;
//     }

//     // ── JSON helper ───────────────────────────────────────────────

//     public static JsonNode jsonb(Map<String, Object> map) {
//         return OM.valueToTree(map);
//     }

//     private TestFixtures() {}
// }
