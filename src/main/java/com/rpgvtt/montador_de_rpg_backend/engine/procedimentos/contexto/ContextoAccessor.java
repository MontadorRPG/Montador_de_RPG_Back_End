package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos.contexto;

import java.util.*;

/**
 * Type-safe wrapper around the procedure's contexto map.
 * Lives on ProcedimentoContexto and is the ONLY way handlers read from contexto.
 * Handlers write to contexto only via put() which accepts Object but documents
 * the expected type in the key contract.
 * Rule: contexto may only hold primitives, Strings, Lists of primitives,
 * and Maps of primitives. Never entity objects — those always go through
 * InstanciaResolver.
 */
public class ContextoAccessor {

    private final Map<String, Object> dados;

    public ContextoAccessor(Map<String, Object> dados) {
        this.dados = dados;
    }

    // ── Writes ────────────────────────────────────────────────────

    public void put(String chave, Object valor) {
        dados.put(chave, valor);
    }

    public void putAll(Map<String, Object> map) {
        dados.putAll(map);
    }

    public void remove(String chave) {
        dados.remove(chave);
    }

    public boolean containsKey(String chave) {
        return dados.containsKey(chave);
    }

    // ── Typed reads ───────────────────────────────────────────────

    public Optional<Long> getLong(String chave) {
        Object v = dados.get(chave);
        if (v == null)             return Optional.empty();
        if (v instanceof Long l)   return Optional.of(l);
        if (v instanceof Number n) return Optional.of(n.longValue());
        throw new ContextoTipoException(chave, "Long", v);
    }

    public long getLongOrDefault(String chave, long padrao) {
        return getLong(chave).orElse(padrao);
    }

    public Optional<String> getString(String chave) {
        Object v = dados.get(chave);
        if (v == null)             return Optional.empty();
        if (v instanceof String s) return Optional.of(s);
        throw new ContextoTipoException(chave, "String", v);
    }

    public Optional<Boolean> getBoolean(String chave) {
        Object v = dados.get(chave);
        if (v == null)              return Optional.empty();
        if (v instanceof Boolean b) return Optional.of(b);
        throw new ContextoTipoException(chave, "Boolean", v);
    }

    public Optional<Integer> getInt(String chave) {
        Object v = dados.get(chave);
        if (v == null)             return Optional.empty();
        if (v instanceof Number n) return Optional.of(n.intValue());
        throw new ContextoTipoException(chave, "Integer", v);
    }

    public List<Long> getListaIds(String chave) {
        Object v = dados.get(chave);
        if (v == null) return List.of();
        if (v instanceof List<?> l) {
            // Defensive: coerce Number elements to Long (Jackson deserializes as Integer)
            return l.stream()
                    .map(e -> e instanceof Number n ? n.longValue() : null)
                    .filter(Objects::nonNull)
                    .toList();
        }
        throw new ContextoTipoException(chave, "List<Long>", v);
    }

    @SuppressWarnings ("unchecked")
    public List<Object> getList(String chave) {
        Object v = dados.get(chave);

        if (v == null) {
            List<Object> novaLista = new ArrayList<>();
            dados.put(chave, novaLista);
            return novaLista;
        }

        if (v instanceof List<?> l) {

            if (l instanceof ArrayList) {
                return (List<Object>) l;
            }
            
            List<Object> listaMutavel = new ArrayList<>(l);
            dados.put(chave, listaMutavel);
            return listaMutavel;
        }

        throw new IllegalStateException("O campo '" + chave + "' não é uma lista. Tipo encontrado: " + v.getClass().getName());
    }

    public Set<String> keySet() {
        return this.dados.keySet();
    }

    public <T> Optional<T> get(String chave, Class<T> tipo) {
        Object v = dados.get(chave);
        if (v == null)          return Optional.empty();
        if (tipo.isInstance(v)) return Optional.of(tipo.cast(v));
        throw new ContextoTipoException(chave, tipo.getSimpleName(), v);
    }

    public Map<String, Object> copyKeys(List<String> keys) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (String key : keys) {
            Object valor = dados.get(key);
            if (valor != null) {
                resultado.put(key, valor);
            }
        }
        return resultado;
    }

    // ── Required reads (throw if absent) ─────────────────────────

    public Long getLongOrThrow(String chave) {
        return getLong(chave).orElseThrow(() ->
                new IllegalStateException("Chave obrigatória ausente no contexto: '" + chave + "'"));
    }

    public String getStringOrThrow(String chave) {
        return getString(chave).orElseThrow(() ->
                new IllegalStateException("Chave obrigatória ausente no contexto: '" + chave + "'"));
    }

    // For snapshot serialization — ProcedimentoContexto calls this
    public Map<String, Object> dados() {
        return Collections.unmodifiableMap(dados);
    }

    public static class ContextoTipoException extends RuntimeException {
        public ContextoTipoException(String chave, String tipoEsperado, Object valorReal) {
            super("Contexto['" + chave + "']: esperado " + tipoEsperado +
                    ", encontrado " + valorReal.getClass().getSimpleName() +
                    " = " + valorReal);
        }
    }
}