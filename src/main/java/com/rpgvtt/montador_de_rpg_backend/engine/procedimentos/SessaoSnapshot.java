package com.rpgvtt.montador_de_rpg_backend.engine.procedimentos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessaoSnapshot {

    private List<ProcedimentoSnapshot> pilha = new ArrayList<>();
}
