package com.rpgvtt.montador_de_rpg_backend.domain.model.sessao;

import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Sessao {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sessao_seq"
    )
    @SequenceGenerator(
            name = "sessao_seq",
            sequenceName = "sessao_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_campanha")
    private Campanha campanha;

    @CreationTimestamp
    @Column(name = "data_inicio", nullable = false, updatable = false)
    private LocalDateTime dataInicio;

    @CreationTimestamp
    @Column(name = "data_fim", nullable = false, updatable = false)
    private LocalDateTime dataFim;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sessao")
    private List<Cena> cenas;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sessao")
    private List<HistoricoAcoes> historicoAcoes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sessao")
    private List<MensagemLog> mensagens;


}
