// package com.rpgvtt.montador_de_rpg_backend.domain.engine;

// import com.rpgvtt.montador_de_rpg_backend.domain.engine.components.FormulaReferencer;
// import com.rpgvtt.montador_de_rpg_backend.domain.engine.utils.DiceExtractor;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.personagem.Personagem;
// import org.springframework.stereotype.Service;
// import java.util.List;

// // Não faz tanto sentido colcoar isso aqui.
// // O problema é que esta hardcodando a batalha.
// // a batalha ficaria a cargo dos serives de procedimento/etapas e dos servies de efeitos e entidades.

// @Service
// public class EngineBattleMaster {

//     /**
//      * O método mestre que coordena a ação de ataque na área tática
//      */
//     public void executarAcaoNaArea(String formulaHabilidade, Personagem atacante, List<Personagem> alvosDetetadosNaGrid) {
        
//         // 1. Usa o UTILS para verificar se precisamos paralisar o turno e pedir dados ao Player
//         List<String> dadosNecessarios = DiceExtractor.extrairDados(formulaHabilidade);
        
//         if (!dadosNecessarios.isEmpty()) {
//             // [AQUI ENTRA O WEBSOCKET]: Dispara o evento pro front: "Player, role um " + dadosNecessarios
//             System.out.println("Ação paralisada! Aguardando o player rodar na física do front: " + dadosNecessarios);
//             return; 
//         }

//         // 2. Se não precisa de dados (ou se o front já devolveu o resultado do dado),
//         // o COMPONENT processa a regra contra cada inimigo pego na área quadrada
//         for (Personagem alvo : alvosDetetadosNaGrid) {
//             FormulaReferencer.processarEExecutar(formulaHabilidade, atacante.getAtributos(), alvo.getAtributos());
//         }
//     }
// }