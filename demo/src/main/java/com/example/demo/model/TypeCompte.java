package com.example.demo.model;

public enum TypeCompte {
    COURANT("Compte Courant"),
    EPARGNE("Compte Épargne"),
    PEL("Plan Épargne Logement");

    private final String libelle;

    TypeCompte(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
