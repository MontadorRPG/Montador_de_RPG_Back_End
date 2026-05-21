// domain/validation/schema/CampoCompostoSchema.java
package com.rpgvtt.montador_de_rpg_backend.domain.validation.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Representa um campo dentro de um atributo composto.
// Ex: dentro de "for" existem "valor" e "modificador"
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampoCompostoSchema {

    private String tipo;
    private boolean derivado;
    private String formula;
    private Integer min;
    private Integer max;
    private Object defaultVal;

    public String getTipo()      { return tipo; }
    public boolean isDerivado()   { return derivado; }
    public String getFormula()   { return formula; }
    public Integer getMin()      { return min; }
    public Integer getMax()      { return max; }
    public Object getDefaultVal(){ return defaultVal; }
}