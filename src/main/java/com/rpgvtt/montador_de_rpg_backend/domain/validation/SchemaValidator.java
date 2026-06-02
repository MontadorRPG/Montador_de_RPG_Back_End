// domain/validation/SchemaValidator.java
package com.rpgvtt.montador_de_rpg_backend.domain.validation;

import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeSistema;
import com.rpgvtt.montador_de_rpg_backend.domain.model.sistema.Sistema;
import com.rpgvtt.montador_de_rpg_backend.domain.validation.exception.*;
import com.rpgvtt.montador_de_rpg_backend.domain.validation.schema.*;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class SchemaValidator {

    private final ObjectMapper objectMapper;

    public SchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // valida a consistência interna dos schemas do sistema
    // Chamado ao criar ou editar um Sistema.
    public void validarConsistenciaSchemas(Sistema sistema) {

        Map<String, AtributoSchema> schemaAtributos = converterSchemaAtributos(sistema);
        Map<String, TipoEntidadeSchema> schemaEntidades = converterSchemaEntidades(sistema);

        for (Map.Entry<String, TipoEntidadeSchema> entry : schemaEntidades.entrySet()) {
            String nomesTipo = entry.getKey();
            TipoEntidadeSchema tipoSchema = entry.getValue();

            // Verifica se cada atributo que o tipo usa existe em schema_atributos
            if (tipoSchema.getAtributos() != null) {
                for (String nomeAtributo : tipoSchema.getAtributos()) {
                    if (!schemaAtributos.containsKey(nomeAtributo)) {
                        
                        throw new SchemaInconsistenteException(nomesTipo, nomeAtributo);
                    }
                }
            }

            if (tipoSchema.getObrigatorios() != null) {
                for (String obrigatorio : tipoSchema.getObrigatorios()) {
                    if (!tipoSchema.getAtributos().contains(obrigatorio)) {
                        throw new SchemaInconsistenteException(
                            nomesTipo,
                            "atributo obrigatório '%s' não está na lista de atributos do tipo"
                            .formatted(obrigatorio)
                        );
                    }
                }
            }
        }
    }

    // valida os atributos de um Entidade
    // Chamado ao criar ou editar uma entidade sistema.
    public void validarEntidade(EntidadeSistema entidade, Sistema sistema) {
        Map<String, AtributoSchema> schemaAtributos = converterSchemaAtributos(sistema);
        Map<String, TipoEntidadeSchema> schemaEntidades = converterSchemaEntidades(sistema);
        Map<String, Object> entidadeAtributos = jsonNodeToMap(entidade.getAtributos());

        TipoEntidadeSchema tipoSchema = resolverTipo(entidade.getTipo(), schemaEntidades);
        validarObrigatorios(entidadeAtributos, tipoSchema);
        validarValoresAtributos(entidadeAtributos, schemaAtributos, tipoSchema);
    }

    // valida os atributos de uma EntidadeInstancia
    // Chamado ao instanciar uma entidade em campanha.
    public void validarInstancia(EntidadeInstancia instancia, Sistema sistema) {
        Map<String, AtributoSchema> schemaAtributos = converterSchemaAtributos(sistema);
        Map<String, TipoEntidadeSchema> schemaEntidades = converterSchemaEntidades(sistema);
        Map<String, Object> instanciaAtributosAtuais = jsonNodeToMap(instancia.getAtributosAtuais());

        TipoEntidadeSchema tipoSchema = resolverTipo(instancia.getTipo(), schemaEntidades);
        validarObrigatorios(instanciaAtributosAtuais, tipoSchema);
        validarValoresAtributos(instanciaAtributosAtuais, schemaAtributos, tipoSchema);
    }


    // MÉTODOS PRIVADOS

    // Verifica se o tipo existe no schema e o retorna.
    // Centraliza a verificação para não repetir em validarTemplate e validarInstancia.
    private TipoEntidadeSchema resolverTipo( String tipo, Map<String, TipoEntidadeSchema> schemaEntidades) {

        TipoEntidadeSchema tipoSchema = schemaEntidades.get(tipo);

        // Se null, o tipo não existe no sistema
        if (tipoSchema == null) {
            throw new TipoEntidadeInvalidoException(tipo);
        }

        return tipoSchema;
    }

    // Verifica se todos os atributos obrigatórios estão presentes no mapa de valores
    private void validarObrigatorios(Map<String, Object> atributos, TipoEntidadeSchema tipoSchema) {

        if (tipoSchema.getObrigatorios() == null) return;

        for (String obrigatorio : tipoSchema.getObrigatorios()) {
            if (!atributos.containsKey(obrigatorio)) {
                throw new AtributoInvalidoException(
                    obrigatorio,
                    "atributo obrigatório ausente"
                );
            }
        }
    }

    // Valida cada atributo presente na entidade contra as regras do schema
    private void validarValoresAtributos(
            Map<String, Object> atributos,
            Map<String, AtributoSchema> schemaAtributos,
            TipoEntidadeSchema tipoSchema
        ) {

        for (Map.Entry<String, Object> entry : atributos.entrySet()) {
            String nomeAtributo = entry.getKey();
            Object valor = entry.getValue();

            // Ignora se tiver atributos não existente no sisteam, pois podem ser customizados
            AtributoSchema meta = schemaAtributos.get(nomeAtributo);
            if (meta == null) continue;

            if (meta.isDerivado()) {
                throw new AtributoInvalidoException(
                    nomeAtributo,
                    "atributo derivado não pode ser informado manualmente"
                );
            }

            // Delega a validação para o método correto conforme o tipo
            switch (meta.getTipo()) {
                case "int"      -> validarInt(nomeAtributo, valor, meta);
                case "enum"     -> validarEnum(nomeAtributo, valor, meta);
                case "array"    -> validarArray(nomeAtributo, valor, meta);
                case "composto" -> validarComposto(nomeAtributo, valor, meta);
                case "bool"     -> validarBool(nomeAtributo, valor);
                // "string" e "mapa" aceitam qualquer valor por ora
            }
        }
    }

    private void validarInt(String nome, Object valor, AtributoSchema meta) {
        
        if (!(valor instanceof Integer i)) {
            throw new AtributoInvalidoException(nome, "deveria ser um número inteiro");
        }
        if (meta.getMin() != null && i < meta.getMin()) {
            throw new AtributoInvalidoException(
                nome,
                "valor %d abaixo do mínimo permitido (%d)".formatted(i, meta.getMin())
            );
        }
        if (meta.getMax() != null && i > meta.getMax()) {
            throw new AtributoInvalidoException(
                nome,
                "valor %d acima do máximo permitido (%d)".formatted(i, meta.getMax())
            );
        }
    }

    private void validarEnum(String nome, Object valor, AtributoSchema meta) {
        if (!(valor instanceof String s)) {
            throw new AtributoInvalidoException(nome, "deveria ser uma string");
        }
        // Verifica se o valor está entre os permitidos
        if (meta.getValoresValidos() != null && !meta.getValoresValidos().contains(s)) {
            throw new AtributoInvalidoException(
                nome,
                "valor '%s' não está entre os permitidos: %s"
                .formatted(s, meta.getValoresValidos())
            );
        }
    }

    private void validarArray(String nome, Object valor, AtributoSchema meta) {
        if (!(valor instanceof List<?> lista)) {
            throw new AtributoInvalidoException(nome, "deveria ser uma lista");
        }
        if (meta.getMinimoItens() != null && lista.size() < meta.getMinimoItens()) {
            throw new AtributoInvalidoException(
                nome,
                "lista tem %d itens, mínimo é %d".formatted(lista.size(), meta.getMinimoItens())
            );
        }
        if (meta.getMaximoItens() != null && lista.size() > meta.getMaximoItens()) {
            throw new AtributoInvalidoException(
                nome,
                "lista tem %d itens, máximo é %d".formatted(lista.size(), meta.getMaximoItens())
            );
        }
        // Verifica cada item da lista se houver itens válidos definidos
        if (meta.getItensValidos() != null) {
            for (Object item : lista) {
                if (!meta.getItensValidos().contains(item.toString())) {
                    throw new AtributoInvalidoException(
                        nome,
                        "item '%s' não está entre os valores permitidos".formatted(item)
                    );
                }
            }
        }
    }

    private void validarComposto(String nome, Object valor, AtributoSchema meta) {
       
        if (!(valor instanceof Map<?, ?> mapa)) {
            throw new AtributoInvalidoException(nome, "deveria ser um objeto");
        }
        if (meta.getCampos() == null) return;

        // Valida cada campo interno do composto
        for (Map.Entry<String, CampoCompostoSchema> campoEntry : meta.getCampos().entrySet()) {
            String nomeCampo = campoEntry.getKey();
            CampoCompostoSchema campoSchema = campoEntry.getValue();

            // Campos derivados dentro do composto também não podem ser informados
            if (campoSchema.isDerivado()) continue;

            Object valorCampo = mapa.get(nomeCampo);
            if (valorCampo == null) continue;

            // Valida tipo e limites do campo interno
            if ("int".equals(campoSchema.getTipo()) && valorCampo instanceof Integer i) {
                if (campoSchema.getMin() != null && i < campoSchema.getMin()) {
                    throw new AtributoInvalidoException(
                        nome + "." + nomeCampo,
                        "valor %d abaixo do mínimo (%d)".formatted(i, campoSchema.getMin())
                    );
                }
                if (campoSchema.getMax() != null && i > campoSchema.getMax()) {
                    throw new AtributoInvalidoException(
                        nome + "." + nomeCampo,
                        "valor %d acima do máximo (%d)".formatted(i, campoSchema.getMax())
                    );
                }
            }
        }
    }

    private void validarBool(String nome, Object valor) {
        if (!(valor instanceof Boolean)) {
            throw new AtributoInvalidoException(nome, "deveria ser true ou false");
        }
    }


    // CONVERSORES — transformam o Map<String, Object> do JSONB em tipos conhecidos

    private Map<String, AtributoSchema> converterSchemaAtributos(Sistema sistema) {
        return objectMapper.convertValue(
            sistema.getSchemaAtributos(),
            new TypeReference<Map<String, AtributoSchema>>() {}
        );
    }

    private Map<String, TipoEntidadeSchema> converterSchemaEntidades(Sistema sistema) {
        return objectMapper.convertValue(
            sistema.getSchemaEntidades(),
            new TypeReference<Map<String, TipoEntidadeSchema>>() {}
        );
    }

    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        return objectMapper.convertValue(node, new TypeReference<>() {});
    }
}