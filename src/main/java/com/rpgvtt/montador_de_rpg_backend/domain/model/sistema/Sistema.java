package com.rpgvtt.montador_de_rpg_backend.domain.model.sistema;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica.Resolucao;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
// import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Sistema {
        @Id
        @GeneratedValue(
                strategy = GenerationType.SEQUENCE,
                generator = "sist_seq"
        )
        @SequenceGenerator(
                name = "sist_seq",
                sequenceName = "sist_sequence",
                allocationSize = 1
        )
        private Long id;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JoinColumn(name = "id_usuario_criador")
        private Usuario usuario;

        @ManyToOne (fetch = FetchType.LAZY)
        @JoinColumn (name = "id_sistema_pai")
        private Sistema sistemaPai;

        @NotNull
        private String nome;

        private String descricao;

        private String urlImagem;

        @NotNull
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(columnDefinition = "jsonb")
        private JsonNode configuracao;

        @NotNull
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(columnDefinition = "jsonb")
        private JsonNode schemaAtributos;

        @NotNull
        @JdbcTypeCode (SqlTypes.JSON)
        @Column(columnDefinition = "jsonb")
        private JsonNode schemaEntidades;

        @NotNull
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(columnDefinition = "jsonb")
        private JsonNode schemaResolucoes;

        @NotNull
        private Integer versaoSchemas;

        @NotNull
        private boolean eOficial;

        @CreationTimestamp
        @Column(name = "criado_em", updatable = false)
        private LocalDateTime criadoEm;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistema")
        private List<Campanha> campanhas;

        // @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistema")
        // private List<EntidadeInstancia> personagens;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistema")
        private List<Procedimento> procedimento;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistema")
        private List<EntidadeSistema> entidadesSistema;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistema")
        private List<EventoSistema> eventos;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistema")
        private List<Resolucao> resolucoes;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "sistemaPai")
        private List<Sistema> sistemasFilhos;


}

// EXEMPLO DE SCHEMAS PARA CONFIGURAÇÃO DO SISTEMA

/*
Shcema Atributos:

{
        "vig": {
                "label": "Vigour",
                "tipo": "int",
                "min": 0,
                "max": 19,
                "descricao": "Força física e resistência"
        },

        "cla": {
                "label": "Clarity",
                "tipo": "int",
                "min": 0,
                "max": 19,
                "descricao": "Instinto e percepção"
        },

        "spi": {
                "label": "Spirit",
                "tipo": "int",
                "min": 0,
                "max": 19,
                "descricao": "Força de vontade e presença"
        },

        "gd": {
                "label": "Guard",
                "tipo": "composto",
                "campos": {
                        "maximo": { "tipo": "int", "min": 0 },
                        "atual":  { "tipo": "int", "min": 0 }
                },
                "descricao": "Capacidade de evitar ferimentos"
        },

        "armadura": {
                "label": "Armour",
                "tipo": "int",
                "min": 0,
                "max": 6,
                "derivado": true,
                "formula": {
                        "tipo": "funcao",
                        "nome": "sumList",
                        "argumentos": [{
                                "tipo": "caminho",
                                "caminho": "armadura_partes"
                        }]
                }
        },

        "armadura_partes": {
                "label": "Armour Parts",
                "tipo": "array",
                "itens_tipo": "int",
                "descricao": "Valores de armadura de cada peça equipada"
        },

        "glory": {
                "label": "Glory",
                "tipo": "int",
                "min": 0,
                "descricao": "Reputação e progresso do Knight"
        },

        "fatigued": {
                "label": "Fatigued",
                "tipo": "bool",
                "default": false,
                "descricao": "Não pode usar Feats até descansar"
        },

        "exposed": {
                "label": "Exposed",
                "tipo": "bool",
                "default": false,
                "descricao": "Tratado como tendo 0 GD"
        },

        "wounded": {
                "label": "Wounded",
                "tipo": "bool",
                "default": false,
                "descricao": "Sofreu dano além do GD"
        }
}


Shcema Entidades:

{
        "knight": {
                "label": "Knight",
                "atributos": ["vig", "cla", "spi", "gd", "armadura", "armadura_partes", "glory", "fatigued", "exposed", "wounded"],
                "obrigatorios": ["vig", "cla", "spi", "gd"]
        },
        "npc": {
                "label": "NPC / Creature",
                "atributos": ["vig", "cla", "spi", "gd", "armadura", "armadura_partes", "wounded"],
                "obrigatorios": ["vig", "gd"]
        },
        "warband": {
                "label": "Warband",
                "atributos": ["vig", "cla", "spi", "gd", "armadura"],
                "obrigatorios": ["vig", "cla", "spi", "gd"]
        },
        "weapon": {
                "label": "Weapon",
                "atributos": [],
                "obrigatorios": []
        },
        "armour_piece": {
                "label": "Armour Piece",
                "atributos": [],
                "obrigatorios": []
        }
}

Shcema Resoluções:

{
        "virtude_save": {
                "label": "Teste de Virtude",
                "descricao": "Rola d20 <= atributo",
                "parametros": {
                        "atributo": {
                        "tipo": "referencia_atributo",
                        "obrigatorio": true,
                        "atributos_permitidos": ["vig", "cla", "spi"],
                        "descricao": "Nome do atributo (vig, cla, spi)"
                },
                "sucesso_critico": {
                        "tipo": "int",
                        "min": 1,
                        "max": 20,
                        "padrao": 1,
                },
                "falha_critica": {
                        "tipo": "int",
                        "min": 1,
                        "max": 20,
                        "padrao": 20
                }
        }

        "luck_roll": {
                "label": "Rolagem de Sorte (Luck Roll)",
                "descricao": "Rola 1d6 e consulta a tabela de sorte.",
                "parametros": {
                        "dado": {
                                "tipo": "enum",
                                "valores": ["d6"],
                                "padrao": "d6"
                        },
                        "tabela": {
                                "tipo": "array",
                                "obrigatorio": true,
                                "item_schema": {
                                        "tipo": "composto",
                                        "campos": {
                                                "min": { "tipo": "int", "min": 1, "max": 6 },
                                                "max": { "tipo": "int", "min": 1, "max": 6 },
                                                "resultado": {
                                                "tipo": "enum",
                                                "valores": ["Crise", "Problema", "Bênção"]
                                                },
                                                "descricao": { "tipo": "string", "obrigatorio": false }
                                        }
                                }
                        }
                }
        }
}

OBS: Não existem regras de sucesso ou falha critica no Mythic Bastionlad, isso é apenas um exemplo.
*/
