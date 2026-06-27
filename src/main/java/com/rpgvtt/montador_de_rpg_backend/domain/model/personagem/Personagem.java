package com.rpgvtt.montador_de_rpg_backend.domain.model.personagem;

// import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.campanha.Campanha;
import com.rpgvtt.montador_de_rpg_backend.domain.model.entidade.EntidadeInstancia;
import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
// import org.hibernate.annotations.JdbcTypeCode;
// import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Personagem {
        @Id
        @GeneratedValue(
                strategy = GenerationType.SEQUENCE,
                generator = "pers_seq"
        )
        @SequenceGenerator(
                name = "pers_seq",
                sequenceName = "pers_sequence",
                allocationSize = 1
        )
        private Long id;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JoinColumn(name = "id_usuario")
        private Usuario usuario;

        @OneToOne (cascade = CascadeType.ALL)
        @JoinColumn
        private EntidadeInstancia instancia;

        @ManyToOne(optional = true, fetch = FetchType.LAZY)
        @JoinColumn(name = "id_campanha")
        private Campanha campanha;

        private String historia;

        private boolean ativo;

        private String aparencia;

        private String urlImagem;

        @Column(name = "notas_jogador")
        private String notasJogador;

        @CreationTimestamp
        @Column(name = "criado_em")
        private LocalDateTime criadoEm;

    // public JsonNode getAtributos() {
    //     throw new UnsupportedOperationException("Unimplemented method 'getAtributos'");
    // }

}
