// package com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica;

// // import com.rpgvtt.montador_de_rpg_backend.domain.enums.TipoVantagem;
// import jakarta.persistence.*;
// import jakarta.validation.constraints.NotNull;
// import lombok.AllArgsConstructor;
// import lombok.NoArgsConstructor;
// import lombok.Setter;
// import org.hibernate.annotations.JdbcTypeCode;
// import org.hibernate.type.SqlTypes;
// import tools.jackson.databind.JsonNode;

// import java.util.List;

// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// @Entity
// @Table(name = "Rolagem")
// public class Rolagem {
//     @Id
//     @GeneratedValue(
//             strategy = GenerationType.SEQUENCE,
//             generator = "evento_sistema_seq"
//     )
//     @SequenceGenerator(
//             name = "evento_sistema_seq",
//             sequenceName = "evento_sistema_sequence",
//             allocationSize = 1
//     )
//     @Column(name = "id_rolagem")
//     private Long id;

//     @NotNull
//     private String dado;

//     @NotNull
//     private Integer quantidade;

//     @NotNull
//     private Boolean explosao;

//     @JdbcTypeCode(SqlTypes.JSON)
//     @Column(columnDefinition = "jsonb")
//     private JsonNode valorAgregado;

//     @OneToMany(mappedBy = "rolagem")
//     private List<EntidadeEfeito> entidadeEfeitos;

//     @OneToMany(mappedBy = "rolagem")
//     private List<Resolucao> resolucao;
// }
