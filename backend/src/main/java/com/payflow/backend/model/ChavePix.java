package com.payflow.backend.model;

import com.payflow.backend.model.enums.TipoChavePix;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "chaves_pix")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChavePix {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String chave;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoChavePix tipo;

    @ManyToOne
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;
}
