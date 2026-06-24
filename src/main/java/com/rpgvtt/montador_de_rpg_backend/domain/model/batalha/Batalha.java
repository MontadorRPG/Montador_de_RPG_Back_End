// package com.rpgvtt.montador_de_rpg_backend.domain.model.batalha;

// import com.rpgvtt.montador_de_rpg_backend.domain.enums.BatalhaStatus;
// import com.rpgvtt.montador_de_rpg_backend.domain.model.sessao.Sessao;
// import jakarta.persistence.*;
// import lombok.AllArgsConstructor;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// import java.util.List;

// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// @Entity
// public class Batalha {
//     @Id
//     @GeneratedValue(
//             strategy = GenerationType.SEQUENCE,
//             generator = "batalha_seq"
//     )
//     @SequenceGenerator(
//             name = "batalha_seq",
//             sequenceName = "batalha_sequence",
//             allocationSize = 1
//     )
//     private Long id;

//     @ManyToOne(optional = false, fetch = FetchType.LAZY)
//     @JoinColumn(name = "id_sessao")
//     private Sessao sessao;

//     @Column(name = "rodada_atual")
//     private int rodadaAtual;

//     @Column(name = "status")
//     @Enumerated(EnumType.STRING)
//     private BatalhaStatus status;

//     @OneToMany(mappedBy = "batalha", cascade =  CascadeType.ALL, orphanRemoval = true)
//     @OrderBy("ordemIniciativa ASC")
//     private List<BatalhaParticipantes> participantes;

//     public List<BatalhaParticipantes> participantesAtivos() {
//         return participantes.stream()
//                 .filter(BatalhaParticipantes::isAtivo)
//                 .toList();
//     }

//     public List<BatalhaParticipantes> participantesLado(int lado) {
//         return participantes.stream()
//                 .filter(p -> p.getLado() == lado && p.isAtivo())
//                 .toList();
//     }

//     public boolean haParticipanteAtivoNoLado(int lado) {
//         return participantes.stream()
//                 .anyMatch(p -> p.getLado() == lado && p.isAtivo());
//     }
// }
