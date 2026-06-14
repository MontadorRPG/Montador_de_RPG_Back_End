package com.rpgvtt.montador_de_rpg_backend.engine.unit;

import com.rpgvtt.montador_de_rpg_backend.engine.components.InterpretadorJson;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.Contexto;
import com.rpgvtt.montador_de_rpg_backend.engine.utils.ResultadoExpressao;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InterpretadorJson.
 * No Spring context — pure logic.
 *
 * Contexto is implemented inline as a lambda since it's a functional interface.
 */
class InterpretadorJsonTest {

    private InterpretadorJson interpretador;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setup() {
        interpretador = new InterpretadorJson();
    }

    // ── helpers ──────────────────────────────────────────────────

    private JsonNode json(String raw) {
        try { return om.readTree(raw); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private Contexto ctx(Map<String, Object> data) {
        return caminho -> Optional.ofNullable(resolveInMap(data, caminho));
    }

    private Object resolveInMap(Map<String, Object> data, String caminho) {
        String[] partes = caminho.split("\\.");
        Object atual = data;
        for (String parte : partes) {
            if (atual instanceof Map<?, ?> m) atual = m.get(parte);
            else return null;
        }
        return atual;
    }

    // ── constante ─────────────────────────────────────────────────

    @Nested @DisplayName("constante")
    class Constante {

        @Test void retorna_numero() {
            var resultado = interpretador.interpretar(
                    json("""
                            { "tipo":"constante","valor":42 }
                            """), ctx(Map.of()));
            assertThat(resultado.comoNumero()).isEqualTo(42.0);
        }

        @Test void retorna_texto() {
            var resultado = interpretador.interpretar(
                    json("""
                            {"tipo":"constante","valor":"olá" }
                            """), ctx(Map.of()));
            assertThat(resultado.comoTexto()).isEqualTo("olá");
        }

        @Test void retorna_booleano() {
            var resultado = interpretador.interpretar(
                    json("""
                            {"tipo":"constante","valor":true }
                            """), ctx(Map.of()));
            assertThat(resultado.comoBooleano()).isTrue();
        }
    }

    // ── caminho ───────────────────────────────────────────────────

    @Nested @DisplayName("caminho")
    class Caminho {

        @Test void resolve_atributo_simples() {
            var ctx = ctx(Map.of("hp", 30));
            var resultado = interpretador.interpretar(
                    json("""
                            { "tipo":"caminho",
                            "caminho":"hp" }
                            """), ctx);
            assertThat(resultado.comoNumero()).isEqualTo(30.0);
        }

        @Test void resolve_atributo_aninhado() {
            var ctx = ctx(Map.of("hp", Map.of("atual", 20, "maximo", 30)));
            var resultado = interpretador.interpretar(
                    json("""
                            { "tipo":"caminho",
                            "caminho":"hp.atual" }
                            """), ctx);
            assertThat(resultado.comoNumero()).isEqualTo(20.0);
        }

        @Test void usa_padrao_quando_ausente() {
            var resultado = interpretador.interpretar(
                    json("""
                          { "tipo":"caminho",
                          "caminho":"inexistente",
                          "padrao": {"tipo":"constante","valor":10 }}
                          """),
                    ctx(Map.of()));
            assertThat(resultado.comoNumero()).isEqualTo(10.0);
        }

        @Test void lança_quando_ausente_sem_padrao() {
            assertThatThrownBy(() ->
                    interpretador.interpretar(
                            json("""
                                    { "tipo":"caminho","caminho":"inexistente" }
                                    """),
                            ctx(Map.of()))
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inexistente");
        }
    }

    // ── formula ───────────────────────────────────────────────────

    @Nested @DisplayName("formula")
    class Formula {

        @Test void soma_atributo_mais_constante() {
            var ctx = ctx(Map.of("forca", 18));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo": "formula",
                  "operandoA": { "tipo":"caminho","caminho":"forca" },
                  "operador": "-",
                  "operandoB": { "tipo":"constante","valor":10 }
                }"""), ctx);
            assertThat(resultado.comoNumero()).isEqualTo(8.0);
        }

        @Test void calcula_modificador_dnd_via_formula() {
            // (forca - 10) / 2 = (18 - 10) / 2 = 4
            var ctx = ctx(Map.of("forca", 18));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo": "formula",
                  "operandoA": {
                    "tipo": "formula",
                    "operandoA": { "tipo":"caminho","caminho":"forca" },
                    "operador": "-",
                    "operandoB": { "tipo":"constante","valor":10 }
                  },
                  "operador": "/",
                  "operandoB": { "tipo":"constante","valor":2 }
                }"""), ctx);
            assertThat(resultado.comoNumero()).isEqualTo(4.0);
        }

        @Test void potenciacao() {
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"formula",
                  "operandoA":{"tipo":"constante","valor":2},
                  "operador":"^",
                  "operandoB":{"tipo":"constante","valor":8}
                }"""), ctx(Map.of()));
            assertThat(resultado.comoNumero()).isEqualTo(256.0);
        }
    }

    // ── condicao ──────────────────────────────────────────────────

    @Nested @DisplayName("condicao")
    class Condicao {

        @Test void hp_menor_que_metade_retorna_true() {
            var ctx = ctx(Map.of("hp", Map.of("atual", 10, "maximo", 30)));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"condicao",
                  "operandoA":{"tipo":"caminho","caminho":"hp.atual"},
                  "operador":"<",
                  "operandoB":{
                    "tipo":"formula",
                    "operandoA":{"tipo":"caminho","caminho":"hp.maximo"},
                    "operador":"/",
                    "operandoB":{"tipo":"constante","valor":2}
                  }
                }"""), ctx);
            assertThat(resultado.comoBooleano()).isTrue();
        }

        @Test void comparacao_texto_igualdade() {
            var ctx = ctx(Map.of("status", "envenenado"));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"condicao",
                  "operandoA":{"tipo":"caminho","caminho":"status"},
                  "operador":"==",
                  "operandoB":{"tipo":"constante","valor":"envenenado"}
                }"""), ctx);
            assertThat(resultado.comoBooleano()).isTrue();
        }

        @Test void tipos_incompativeis_lancam_excecao() {
            var ctx = ctx(Map.of("hp", 10, "nome", "Thorin"));
            assertThatThrownBy(() ->
                    interpretador.interpretar(json("""
                    {
                      "tipo":"condicao",
                      "operandoA":{"tipo":"caminho","caminho":"hp"},
                      "operador":"==",
                      "operandoB":{"tipo":"caminho","caminho":"nome"}
                    }"""), ctx)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("incompatíveis");
        }
    }

    // ── condicao_composta ─────────────────────────────────────────

    @Nested @DisplayName("condicao_composta")
    class CondicaoComposta {

        @Test void E_ambas_verdadeiras() {
            var ctx = ctx(Map.of("hp", 10, "forca", 15));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"condicao_composta",
                  "operador":"E",
                  "condicoes":[
                    {"tipo":"condicao","operandoA":{"tipo":"caminho","caminho":"hp"},
                     "operador":">","operandoB":{"tipo":"constante","valor":0}},
                    {"tipo":"condicao","operandoA":{"tipo":"caminho","caminho":"forca"},
                     "operador":">=","operandoB":{"tipo":"constante","valor":10}}
                  ]
                }"""), ctx);
            assertThat(resultado.comoBooleano()).isTrue();
        }

        @Test void E_uma_falsa_retorna_false_com_curtocircuito() {
            // forca >= 10 is false → should short-circuit and not evaluate hp
            var ctx = ctx(Map.of("hp", 10, "forca", 5));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"condicao_composta",
                  "operador":"E",
                  "condicoes":[
                    {"tipo":"condicao","operandoA":{"tipo":"caminho","caminho":"forca"},
                     "operador":">=","operandoB":{"tipo":"constante","valor":10}},
                    {"tipo":"condicao","operandoA":{"tipo":"caminho","caminho":"hp"},
                     "operador":">","operandoB":{"tipo":"constante","valor":0}}
                  ]
                }"""), ctx);
            assertThat(resultado.comoBooleano()).isFalse();
        }

        @Test void OU_uma_verdadeira_retorna_true() {
            var ctx = ctx(Map.of("hp", 0, "mana", 5));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"condicao_composta",
                  "operador":"OU",
                  "condicoes":[
                    {"tipo":"condicao","operandoA":{"tipo":"caminho","caminho":"hp"},
                     "operador":">","operandoB":{"tipo":"constante","valor":0}},
                    {"tipo":"condicao","operandoA":{"tipo":"caminho","caminho":"mana"},
                     "operador":">","operandoB":{"tipo":"constante","valor":0}}
                  ]
                }"""), ctx);
            assertThat(resultado.comoBooleano()).isTrue();
        }
    }

    // ── filtro ────────────────────────────────────────────────────

    @Nested @DisplayName("filtro")
    class Filtro {

        @Test void filtra_inimigos_vivos() {
            var inimigos = List.of(
                    Map.of("nome", "Goblin A", "hp", 7),
                    Map.of("nome", "Goblin B", "hp", 0),  // dead
                    Map.of("nome", "Troll",    "hp", 20)
            );
            var ctx = ctx(Map.of("inimigos", inimigos));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"filtro",
                  "lista":{"tipo":"caminho","caminho":"inimigos"},
                  "condicao":{
                    "tipo":"condicao",
                    "operandoA":{"tipo":"caminho","caminho":"item.hp"},
                    "operador":">",
                    "operandoB":{"tipo":"constante","valor":0}
                  }
                }"""), ctx);

            assertThat(resultado.comoLista()).hasSize(2);
        }

        @Test void filtra_com_referencia_a_contexto_externo() {
            // Filter enemies whose hp < limiar defined in outer context
            var inimigos = List.of(
                    Map.of("hp", 3),
                    Map.of("hp", 8)
            );
            var ctx = ctx(Map.of("inimigos", inimigos, "limiar", 5));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"filtro",
                  "lista":{"tipo":"caminho","caminho":"inimigos"},
                  "condicao":{
                    "tipo":"condicao",
                    "operandoA":{"tipo":"caminho","caminho":"item.hp"},
                    "operador":"<",
                    "operandoB":{"tipo":"caminho","caminho":"limiar"}
                  }
                }"""), ctx);

            // Goblin with hp=3 survives the filter; hp=8 does not
            assertThat(resultado.comoLista()).hasSize(1);
        }
    }

    // ── funcao ────────────────────────────────────────────────────

    @Nested @DisplayName("funcao")
    class Funcao {

        @Test void size_retorna_tamanho_da_lista() {
            var ctx = ctx(Map.of("itens", List.of("a", "b", "c")));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"funcao","nome":"size",
                  "argumentos":[{"tipo":"caminho","caminho":"itens"}]
                }"""), ctx);
            assertThat(resultado.comoNumero()).isEqualTo(3.0);
        }

        @Test void sumList_soma_campo_de_cada_item() {
            var itens = List.of(
                    Map.of("preco", 10),
                    Map.of("preco", 5),
                    Map.of("preco", 3)
            );
            var ctx = ctx(Map.of("inventario", itens));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"funcao","nome":"sumList",
                  "argumentos":[
                    {"tipo":"caminho","caminho":"inventario"},
                    {"tipo":"caminho","caminho":"item.preco"}
                  ]
                }"""), ctx);
            assertThat(resultado.comoNumero()).isEqualTo(18.0);
        }

        @Test void averageList_media_de_hp_de_inimigos_vivos() {
            // Tests the critical bug fix: item.hp must resolve per item, not in outer ctx
            var inimigos = List.of(
                    Map.of("hp", 10),
                    Map.of("hp", 6)
            );
            var ctx = ctx(Map.of("inimigos", inimigos));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"funcao","nome":"averageList",
                  "argumentos":[
                    {"tipo":"caminho","caminho":"inimigos"},
                    {"tipo":"caminho","caminho":"item.hp"}
                  ]
                }"""), ctx);
            assertThat(resultado.comoNumero()).isEqualTo(8.0);
        }

        @Test void averageList_lista_vazia_retorna_zero() {
            var ctx = ctx(Map.of("inimigos", List.of()));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"funcao","nome":"averageList",
                  "argumentos":[
                    {"tipo":"caminho","caminho":"inimigos"},
                    {"tipo":"caminho","caminho":"item.hp"}
                  ]
                }"""), ctx);
            assertThat(resultado.comoNumero()).isEqualTo(0.0);
        }

        @Test void clamp_limita_valor() {
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"funcao","nome":"clamp",
                  "argumentos":[
                    {"tipo":"constante","valor":150},
                    {"tipo":"constante","valor":0},
                    {"tipo":"constante","valor":100}
                  ]
                }"""), ctx(Map.of()));
            assertThat(resultado.comoNumero()).isEqualTo(100.0);
        }

        @Test void size_de_filtro_composto_conta_vivos() {
            // Real use case from REPETIR_SE combat loop condition
            var inimigos = List.of(
                    Map.of("hp", 7),
                    Map.of("hp", 0),
                    Map.of("hp", 12)
            );
            var ctx = ctx(Map.of("inimigos", inimigos));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"funcao","nome":"size",
                  "argumentos":[{
                    "tipo":"filtro",
                    "lista":{"tipo":"caminho","caminho":"inimigos"},
                    "condicao":{
                      "tipo":"condicao",
                      "operandoA":{"tipo":"caminho","caminho":"item.hp"},
                      "operador":">",
                      "operandoB":{"tipo":"constante","valor":0}
                    }
                  }]
                }"""), ctx);
            assertThat(resultado.comoNumero()).isEqualTo(2.0);
        }
    }

    // ── ternario ──────────────────────────────────────────────────

    @Nested @DisplayName("ternario")
    class Ternario {

        @Test void retorna_entao_quando_verdadeiro() {
            var ctx = ctx(Map.of("hp", 5, "hp_max", 30));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"ternario",
                  "condicao":{
                    "tipo":"condicao",
                    "operandoA":{"tipo":"caminho","caminho":"hp"},
                    "operador":"<=",
                    "operandoB":{
                      "tipo":"formula",
                      "operandoA":{"tipo":"caminho","caminho":"hp_max"},
                      "operador":"/",
                      "operandoB":{"tipo":"constante","valor":4}
                    }
                  },
                  "entao":{"tipo":"constante","valor":"Crítico"},
                  "senao":{"tipo":"constante","valor":"Normal"}
                }"""), ctx);
            assertThat(resultado.comoTexto()).isEqualTo("Crítico");
        }
    }

    // ── texto ─────────────────────────────────────────────────────

    @Nested @DisplayName("texto")
    class Texto {

        @Test void interpola_variaveis() {
            var ctx = ctx(Map.of("personagem", Map.of("nome", "Thorin"), "dano", 10));
            var resultado = interpretador.interpretar(json("""
                {
                  "tipo":"texto",
                  "template":"{nome} causa {dano} pontos de dano!",
                  "variaveis":{
                    "nome":{"tipo":"caminho","caminho":"personagem.nome"},
                    "dano":{"tipo":"caminho","caminho":"dano"}
                  }
                }"""), ctx);
            assertThat(resultado.comoTexto())
                    .isEqualTo("Thorin causa 10.0 pontos de dano!");
        }
    }
}
