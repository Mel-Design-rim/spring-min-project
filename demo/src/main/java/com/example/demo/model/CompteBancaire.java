package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "comptes_bancaires")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompteBancaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le numéro de compte est obligatoire")
    @Size(min = 5, max = 20, message = "Le numéro de compte doit contenir entre 5 et 20 caractères")
    @Column(unique = true, nullable = false, length = 20)
    private String numeroCompte;

    @NotBlank(message = "Le nom du titulaire est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom du titulaire doit contenir entre 2 et 100 caractères")
    @Column(nullable = false, length = 100)
    private String nomTitulaire;

    @NotNull(message = "Le solde est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "Le solde ne peut pas être négatif")
    @Digits(integer = 17, fraction = 2, message = "Le solde doit être un montant valide")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal solde;

    @NotNull(message = "Le type de compte est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeCompte typeCompte;

    @Column(nullable = false)
    private LocalDateTime dateOuverture;

    @OneToMany(mappedBy = "compteBancaire", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Operation> operations;

    @PrePersist
    protected void onCreate() {
        if (dateOuverture == null) {
            dateOuverture = LocalDateTime.now();
        }
    }

    public CompteBancaire(String numeroCompte, String nomTitulaire, BigDecimal solde, TypeCompte typeCompte) {
        this.numeroCompte = numeroCompte;
        this.nomTitulaire = nomTitulaire;
        this.solde = solde;
        this.typeCompte = typeCompte;
        this.dateOuverture = LocalDateTime.now();
    }
}