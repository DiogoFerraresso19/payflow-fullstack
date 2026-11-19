package com.payflow.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "contas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_conta", nullable = false, unique = true)
    private String numeroConta;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
}