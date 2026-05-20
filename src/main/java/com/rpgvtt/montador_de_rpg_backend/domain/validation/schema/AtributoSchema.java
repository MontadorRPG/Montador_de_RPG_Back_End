// domain/validation/schema/AtributoSchema.java
package com.rpgvtt.montador_de_rpg_backend.domain.validation.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

// Representa a definição de um atributo em schema_atributos.
// O campo "tipo" determina quais outros campos são relevantes:
//   "int"      -> min, max
//   "enum"     -> valoresValidos
//   "array"    -> itensTipo, itensValidos, minimoItens, maximoItens
//   "composto" -> campos
//   "mapa"     -> chavesValidas, valorTipo
//   "bool"     -> nenhum extra
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtributoSchema {

    private String label;
    private String tipo;
    private boolean derivado;
    private String formula;
    private boolean obrigatorio;
    private Object defaultVal;

    // Usados quando tipo = "int"
    private Integer min;
    private Integer max;

    // Usado quando tipo = "enum"
    private List<String> valoresValidos;

    // Usados quando tipo = "array"
    private String itensTipo;
    private List<String> itensValidos; // null = qualquer valor aceito
    private Integer minimoItens;
    private Integer maximoItens;

    // Usado quando tipo = "composto"
    // Map<nome do campo, schema do campo>
    private Map<String, CampoCompostoSchema> campos;

    // Usados quando tipo = "mapa"
    private List<String> chavesValidas;
    private String valorTipo;

    // Getters
    public String getLabel()                            { return label; }
    public String getTipo()                             { return tipo; }
    public boolean isDerivado()                          { return derivado; }
    public String getFormula()                          { return formula; }
    public boolean isObrigatorio()                      { return obrigatorio; }
    public Object getDefaultVal()                       { return defaultVal; }
    public Integer getMin()                             { return min; }
    public Integer getMax()                             { return max; }
    public List<String> getValoresValidos()             { return valoresValidos; }
    public String getItensTipo()                        { return itensTipo; }
    public List<String> getItensValidos()               { return itensValidos; }
    public Integer getMinimoItens()                     { return minimoItens; }
    public Integer getMaximoItens()                     { return maximoItens; }
    public Map<String, CampoCompostoSchema> getCampos() { return campos; }
    public List<String> getChavesValidas()              { return chavesValidas; }
    public String getValorTipo()                        { return valorTipo; }
}