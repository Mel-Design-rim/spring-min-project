package com.example.demo.service;

import com.example.demo.model.CompteBancaire;
import com.example.demo.model.Operation;
import com.example.demo.model.TypeOperation;

import java.time.LocalDateTime;
import java.time.YearMonth;
import com.example.demo.model.TypeCompte;
import com.example.demo.repository.CompteBancaireRepository;
import com.example.demo.repository.OperationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompteBancaireService {

    private final CompteBancaireRepository compteBancaireRepository;
    private final OperationRepository operationRepository;

    @Autowired
    public CompteBancaireService(CompteBancaireRepository compteBancaireRepository,
            OperationRepository operationRepository) {
        this.compteBancaireRepository = compteBancaireRepository;
        this.operationRepository = operationRepository;
    }
    @Transactional
    public CompteBancaire creerCompte(CompteBancaire compte) {
        if (compte.getSolde() == null) {
            compte.setSolde(BigDecimal.ZERO);
        }

        CompteBancaire compteCree = compteBancaireRepository.save(compte);
        if (compteCree.getSolde().compareTo(BigDecimal.ZERO) > 0) {
            Operation depotInitial = new Operation(
                    LocalDateTime.now(),
                    TypeOperation.DEPOT,
                    compteCree.getSolde(),
                    compteCree,
                    "Dépôt initial lors de la création du compte");
            operationRepository.save(depotInitial);
        }
        return compteCree;
    }

    public Optional<CompteBancaire> trouverCompteParId(Long id) {
        return compteBancaireRepository.findById(id);
    }

    public Page<CompteBancaire> listerTousLesComptes(Pageable pageable) {
        return compteBancaireRepository.findAll(pageable);
    }

    public List<CompteBancaire> listerTousLesComptesPourVirement() {
        return compteBancaireRepository.findAll();
    }

    public CompteBancaire modifierCompte(CompteBancaire compte) {

        if (!compteBancaireRepository.existsById(compte.getId())) {
            throw new IllegalArgumentException("Compte non trouvé avec ID: " + compte.getId());
        }
        return compteBancaireRepository.save(compte);
    }

    public void supprimerCompte(Long id) {
        compteBancaireRepository.deleteById(id);
    }



    @Transactional
    public void effectuerDepot(Long compteId, BigDecimal montant) {
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du dépôt doit être positif.");
        }

        CompteBancaire compte = compteBancaireRepository.findById(compteId)
                .orElseThrow(() -> new IllegalArgumentException("Compte non trouvé: " + compteId));

        compte.setSolde(compte.getSolde().add(montant));
        compteBancaireRepository.save(compte);

        Operation depot = new Operation(
                LocalDateTime.now(),
                TypeOperation.DEPOT,
                montant,
                compte,
                "Dépôt d'un montant de " + montant);
        operationRepository.save(depot);
    }

    @Transactional
    public void effectuerRetrait(Long compteId, BigDecimal montant) {
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du retrait doit être positif.");
        }

        CompteBancaire compte = compteBancaireRepository.findById(compteId)
                .orElseThrow(() -> new IllegalArgumentException("Compte source non trouvé: " + compteId));

        if (compte.getSolde().compareTo(montant) < 0) {
            throw new IllegalStateException("Solde insuffisant pour le retrait.");
        }

        compte.setSolde(compte.getSolde().subtract(montant));
        compteBancaireRepository.save(compte);

        Operation retrait = new Operation(
                LocalDateTime.now(),
                TypeOperation.RETRAIT,
                montant,
                compte,
                "Retrait d'un montant de " + montant);
        operationRepository.save(retrait);
    }

    @Transactional
    public void effectuerVirement(Long idCompteSource, Long idCompteCible, BigDecimal montant) {
        if (idCompteSource.equals(idCompteCible)) {
            throw new IllegalArgumentException("Le compte source et le compte cible ne peuvent pas être identiques.");
        }
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du virement doit être positif.");
        }

        CompteBancaire compteSource = compteBancaireRepository.findById(idCompteSource)
                .orElseThrow(() -> new IllegalArgumentException("Compte source non trouvé: " + idCompteSource));
        CompteBancaire compteCible = compteBancaireRepository.findById(idCompteCible)
                .orElseThrow(() -> new IllegalArgumentException("Compte cible non trouvé: " + idCompteCible));

        if (compteSource.getSolde().compareTo(montant) < 0) {
            throw new IllegalStateException("Solde insuffisant sur le compte source.");
        }


        compteSource.setSolde(compteSource.getSolde().subtract(montant));
        compteBancaireRepository.save(compteSource);


        compteCible.setSolde(compteCible.getSolde().add(montant));
        compteBancaireRepository.save(compteCible);


        Operation operationDebit = new Operation(
                LocalDateTime.now(),
                TypeOperation.VIREMENT_DEBIT,
                montant,
                compteSource,
                "Virement vers compte " + compteCible.getNumeroCompte());
        operationRepository.save(operationDebit);


        Operation operationCredit = new Operation(
                LocalDateTime.now(),
                TypeOperation.VIREMENT_CREDIT,
                montant,
                compteCible,
                "Virement depuis compte " + compteSource.getNumeroCompte());
        operationRepository.save(operationCredit);
    }


    public Page<CompteBancaire> rechercherComptes(String termeRecherche, Pageable pageable) {
        return compteBancaireRepository.findByNomTitulaireContainingIgnoreCaseOrNumeroCompteContainingIgnoreCase(
                termeRecherche, termeRecherche, pageable);
    }


    public Page<Operation> listerOperationsParCompte(Long compteId, Pageable pageable) {
        return operationRepository.findByCompteBancaireIdOrderByDateOperationDesc(compteId, pageable);
    }

    public Optional<CompteBancaire> trouverCompteParNumero(String numeroCompte) {
        return compteBancaireRepository.findByNumeroCompte(numeroCompte);
    }


    public Map<String, Object> obtenirStatistiques() {
        Map<String, Object> stats = new HashMap<>();

        List<CompteBancaire> tousLesComptes = compteBancaireRepository.findAll();


        stats.put("nombreTotalComptes", tousLesComptes.size());


        BigDecimal soldeTotal = tousLesComptes.stream()
                .map(CompteBancaire::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("soldeTotal", soldeTotal);


        BigDecimal soldeMoyen = tousLesComptes.isEmpty() ? BigDecimal.ZERO
                : soldeTotal.divide(BigDecimal.valueOf(tousLesComptes.size()), 2, RoundingMode.HALF_UP);
        stats.put("soldeMoyen", soldeMoyen);


        long nombreOperations = operationRepository.count();
        stats.put("nombreOperations", nombreOperations);


        Map<TypeCompte, Long> repartitionParType = tousLesComptes.stream()
                .collect(Collectors.groupingBy(CompteBancaire::getTypeCompte, Collectors.counting()));
        stats.put("repartitionParType", repartitionParType);

        return stats;
    }


    public Map<String, Object> obtenirStatistiquesMensuelles(Long compteId) {
        Map<String, Object> stats = new HashMap<>();


        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);


        BigDecimal monthlyDeposits = operationRepository
                .sumMontantByCompteBancaireIdAndTypeOperationAndDateOperationBetween(
                        compteId, TypeOperation.DEPOT, startOfMonth, endOfMonth);


        BigDecimal monthlyWithdrawals = operationRepository
                .sumMontantByCompteBancaireIdAndTypeOperationAndDateOperationBetween(
                        compteId, TypeOperation.RETRAIT, startOfMonth, endOfMonth);

        BigDecimal monthlyVirementDebits = operationRepository
                .sumMontantByCompteBancaireIdAndTypeOperationAndDateOperationBetween(
                        compteId, TypeOperation.VIREMENT_DEBIT, startOfMonth, endOfMonth);


        BigDecimal monthlyVirementCredits = operationRepository
                .sumMontantByCompteBancaireIdAndTypeOperationAndDateOperationBetween(
                        compteId, TypeOperation.VIREMENT_CREDIT, startOfMonth, endOfMonth);


        BigDecimal totalMonthlyCredits = monthlyDeposits.add(monthlyVirementCredits);


        BigDecimal totalMonthlyDebits = monthlyWithdrawals.add(monthlyVirementDebits);

        stats.put("monthlyDeposits", totalMonthlyCredits);
        stats.put("monthlyWithdrawals", totalMonthlyDebits);

        return stats;
    }


    public Map<String, Object> obtenirDonneesGraphique(Long compteId) {
        Map<String, Object> chartData = new HashMap<>();


        List<String> labels = new ArrayList<>();
        List<BigDecimal> depositsData = new ArrayList<>();
        List<BigDecimal> withdrawalsData = new ArrayList<>();

        YearMonth currentMonth = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);


            labels.add(month.getMonth().name().substring(0, 3));


            BigDecimal monthlyDeposits = operationRepository
                    .sumMontantByCompteBancaireIdAndTypeOperationAndDateOperationBetween(
                            compteId, TypeOperation.DEPOT, startOfMonth, endOfMonth);
            BigDecimal monthlyVirementCredits = operationRepository
                    .sumMontantByCompteBancaireIdAndTypeOperationAndDateOperationBetween(
                            compteId, TypeOperation.VIREMENT_CREDIT, startOfMonth, endOfMonth);
            BigDecimal totalCredits = monthlyDeposits.add(monthlyVirementCredits);


            BigDecimal monthlyWithdrawals = operationRepository
                    .sumMontantByCompteBancaireIdAndTypeOperationAndDateOperationBetween(
                            compteId, TypeOperation.RETRAIT, startOfMonth, endOfMonth);
            BigDecimal monthlyVirementDebits = operationRepository
                    .sumMontantByCompteBancaireIdAndTypeOperationAndDateOperationBetween(
                            compteId, TypeOperation.VIREMENT_DEBIT, startOfMonth, endOfMonth);
            BigDecimal totalDebits = monthlyWithdrawals.add(monthlyVirementDebits);

            depositsData.add(totalCredits);
            withdrawalsData.add(totalDebits);
        }

        chartData.put("labels", labels);
        chartData.put("depositsData", depositsData);
        chartData.put("withdrawalsData", withdrawalsData);

        return chartData;
    }



    public long obtenirNombreTotalComptes() {
        return compteBancaireRepository.count();
    }

    public long getTotalAccountCount() {
        return obtenirNombreTotalComptes();
    }

    public BigDecimal obtenirSoldeTotalTousComptes() {
        List<CompteBancaire> tousLesComptes = compteBancaireRepository.findAll();
        return tousLesComptes.stream()
                .map(CompteBancaire::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal obtenirSoldeMoyenComptes() {
        List<CompteBancaire> tousLesComptes = compteBancaireRepository.findAll();
        if (tousLesComptes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal soldeTotal = obtenirSoldeTotalTousComptes();
        return soldeTotal.divide(BigDecimal.valueOf(tousLesComptes.size()), 2, RoundingMode.HALF_UP);
    }

    public long obtenirNombreTotalOperations() {
        return operationRepository.count();
    }

    public Map<String, Long> obtenirRepartitionParTypeCompte() {
        List<CompteBancaire> tousLesComptes = compteBancaireRepository.findAll();
        return tousLesComptes.stream()
                .collect(Collectors.groupingBy(
                        compte -> compte.getTypeCompte().toString(),
                        Collectors.counting()));
    }

}