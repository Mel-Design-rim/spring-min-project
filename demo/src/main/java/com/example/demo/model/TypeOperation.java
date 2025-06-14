package com.example.demo.model;

public enum TypeOperation {
    DEPOT("Dépôt"),
    RETRAIT("Retrait"),
    VIREMENT_DEBIT("Virement (Débit)"),
    VIREMENT_CREDIT("Virement (Crédit)");

    private final String libelle;

    TypeOperation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}