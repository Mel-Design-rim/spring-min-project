package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "operations") @Data @NoArgsConstructor @AllArgsConstructor
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dateOperation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeOperation typeOperation;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @Column(length = 255) 
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id", nullable = false)
    private CompteBancaire compteBancaire;

    public Operation(LocalDateTime dateOperation, TypeOperation typeOperation, BigDecimal montant, CompteBancaire compteBancaire, String description) {
        this.dateOperation = dateOperation;
        this.typeOperation = typeOperation;
        this.montant = montant;
        this.compteBancaire = compteBancaire;
        this.description = description;
    }
}
