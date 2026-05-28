// package com.rpgvtt.montador_de_rpg_backend.domain.model.mecanica;

// import jakarta.persistence.*;
// import jakarta.validation.constraints.NotNull;
// import lombok.AllArgsConstructor;
// import lombok.NoArgsConstructor;
// import lombok.Setter;
// import org.hibernate.annotations.JdbcTypeCode;
// import org.hibernate.type.SqlTypes;
// import tools.jackson.databind.JsonNode;

// // Tabela de relação entre entidade efeito e primitivos

// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// @Entity
// @Table (name = "efeitos_primitivos")
// public class EfeitosPrimitivos {

//     @EmbeddedId
//     private EfeitosPrimitivosKey id;

//     @NotNull
//     private int ordem;

//     @NotNull
//     @JdbcTypeCode(SqlTypes.JSON)
//     @Column(columnDefinition = "jsonb")
//     private JsonNode parametros;

//     @ManyToOne(optional = false, fetch = FetchType.LAZY)
//     @MapsId(value = "idEfeito")
//     @JoinColumn(name = "id_efeito")
//     private EntidadeEfeito efeito;

//     @ManyToOne(optional = false, fetch = FetchType.LAZY)
//     @MapsId(value = "idPrimitivo")
//     @JoinColumn(name = "id_primitivo")
//     private Primitivo primitivo;
// }
