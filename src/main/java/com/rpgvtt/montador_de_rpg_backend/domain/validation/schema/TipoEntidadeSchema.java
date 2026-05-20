// domain/validation/schema/TipoEntidadeSchema.java
package com.rpgvtt.montador_de_rpg_backend.domain.validation.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Representa um tipo dentro de schema_entidades.
// Ex: "criatura" -> quais atributos usa e quais são obrigatórios
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipoEntidadeSchema {

    private String label;
    private List<String> atributos;
    private List<String> obrigatorios;

    public String getLabel()                { return label; }
    public List<String> getAtributos()      { return atributos; }
    public List<String> getObrigatorios()   { return obrigatorios; }
}