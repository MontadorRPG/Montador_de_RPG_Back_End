package com.rpgvtt.montador_de_rpg_backend.engine.utils.rolagemEngine;

import com.rpgvtt.montador_de_rpg_backend.dto.mecanica.ResultadoPoolDTO;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class RolagemEngine {

    private final Random rng;

    public RolagemEngine() {
        this.rng = new SecureRandom();
    }

    /**
     * Executa uma Rolagem conforme configuração do banco.
     * Retorna o resultado com breakdown para exibição ao jogador.
     */
    public ResultadoRolagem executar(Rolagem config) {
        int faces = parseFaces(config.dado()); // "d20" → 20
        List<Integer> rolos = new ArrayList<>();

        for (int i = 0; i < config.quantidade(); i++) {
            int resultado = rolarUmDado(faces, config.explosao(), rolos);
            rolos.add(resultado);
        }

        int total = rolos.stream().mapToInt(Integer::intValue).sum();
        return new ResultadoRolagem(config.dado(), rolos, total);
    }

    /**
     * Aplica vantagem/desvantagem: rola duas vezes, pega maior ou menor.
     * Apenas para dados únicos (d20 em D&D, d100 em Call of Cthulhu, etc).
     */
    public ResultadoRolagem executarComVantagem(Rolagem config, VantagemTipo vantagem) {
        if (vantagem == VantagemTipo.NORMAL || config.quantidade() > 1) {
            return executar(config);
        }

        ResultadoRolagem rolo1 = executar(config);
        ResultadoRolagem rolo2 = executar(config);

        if (vantagem == VantagemTipo.VANTAGEM) {
            return rolo1.total() >= rolo2.total() ? rolo1 : rolo2;
        } else { // DESVANTAGEM
            return rolo1.total() <= rolo2.total() ? rolo1 : rolo2;
        }
    }

    // ── Pool de dados ───────────────────────────────────────────────

    /**
     * Rola um pool de N dados e conta sucessos.
     *
     * @param tamanhoPool  quantidade de dados a rolar
     * @param dado         tipo de dado, ex: "d10"
     * @param dificuldade  valor mínimo para sucesso (null = todo dado conta)
     * @param contaUns     se true, dados mostrando 1 cancelam um sucesso (botch)
     * @param criticoEm    "dois_maximos_consecutivos" | "qualquer_maximo" | null
     */
    public ResultadoPoolDTO executarPool(int tamanhoPool,
                                         String dado,
                                         Integer dificuldade,
                                         boolean contaUns,
                                         String criticoEm) {
        int faces = parseFaces(dado);
        List<Integer> rolos = new ArrayList<>();

        for (int i = 0; i < tamanhoPool; i++) {
            rolos.add(rng.nextInt(faces) + 1);
        }

        int sucessos = 0;
        int falhas   = 0; // só relevante quando contaUns = true

        for (int rolo : rolos) {
            if (dificuldade != null) {
                if (rolo >= dificuldade)        sucessos++;
                else if (contaUns && rolo == 1) falhas++;
            } else {
                sucessos++; // sem dificuldade = todo dado conta
            }
        }

        boolean botch = contaUns && falhas > 0 && (sucessos - falhas) < 0;
        int sucessosLiquidos = contaUns ? Math.max(0, sucessos - falhas) : sucessos;
        boolean critico = avaliarCritico(rolos, faces, criticoEm);

        return new ResultadoPoolDTO(tamanhoPool, rolos, sucessosLiquidos, falhas, critico, botch, dificuldade);
    }

    /**
     * Rola um dado único. Se explosão estiver ativa e sair o valor máximo,
     * rola novamente e soma (ex: Savage Worlds, alguns sistemas OSR).
     */
    private int rolarUmDado(int faces, boolean explosao, List<Integer> historico) {
        int resultado = rng.nextInt(faces) + 1;
        historico.add(resultado);

        if (explosao && resultado == faces) {
            resultado += rolarUmDado(faces, true, historico); // recursivo
        }
        return resultado;
    }

    private boolean avaliarCritico(List<Integer> rolos, int faces, String criticoEm) {
        if (criticoEm == null) return false;
        return switch (criticoEm) {
            case "dois_maximos_consecutivos" -> {
                for (int i = 0; i < rolos.size() - 1; i++) {
                    if (rolos.get(i) == faces && rolos.get(i + 1) == faces) yield true;
                }
                yield false;
            }
            case "qualquer_maximo" -> rolos.stream().anyMatch(r -> r == faces);
            default -> false;
        };
    }

    public int parseFaces(String dado) {
        // "d20" → 20, "d6" → 6
        return Integer.parseInt(dado.substring(1));
    }

    public static Rolagem simples(String dado, Integer quantidade, Boolean explosao){
        return new Rolagem(dado, quantidade, explosao);
    }
}

