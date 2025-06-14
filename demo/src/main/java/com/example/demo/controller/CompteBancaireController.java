package com.example.demo.controller;

import com.example.demo.model.CompteBancaire;
import com.example.demo.model.Operation;
import com.example.demo.model.TypeCompte;
import com.example.demo.service.CompteBancaireService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Controller
@RequestMapping("/comptes")
public class CompteBancaireController {

    private static final Logger logger = LoggerFactory.getLogger(CompteBancaireController.class);
    private final CompteBancaireService compteBancaireService;


    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String ERROR_MESSAGE = "errorMessage";

    @Autowired
    public CompteBancaireController(CompteBancaireService compteBancaireService) {
        this.compteBancaireService = compteBancaireService;
        logger.info("CompteBancaireController initialized successfully");
    }


    public static class OperationRequest {
        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
        private BigDecimal montant;

        private String description;


        public BigDecimal getMontant() {
            return montant;
        }

        public void setMontant(BigDecimal montant) {
            this.montant = montant;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class VirementRequest extends OperationRequest {
        @NotNull(message = "Le compte source est obligatoire")
        private Long idCompteSource;

        @NotNull(message = "Le compte destination est obligatoire")
        private Long idCompteCible;


        public Long getIdCompteSource() {
            return idCompteSource;
        }

        public void setIdCompteSource(Long idCompteSource) {
            this.idCompteSource = idCompteSource;
        }

        public Long getIdCompteCible() {
            return idCompteCible;
        }

        public void setIdCompteCible(Long idCompteCible) {
            this.idCompteCible = idCompteCible;
        }
    }


    @GetMapping
    public String listerComptes(Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", defaultValue = "numeroCompte") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction) {

        try {

            page = Math.max(0, page);
            size = Math.min(Math.max(1, size), MAX_PAGE_SIZE);


            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            Sort sort = Sort.by(sortDirection, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<CompteBancaire> pageComptes;

            if (keyword != null && !keyword.trim().isEmpty()) {
                String sanitizedKeyword = keyword.trim();
                logger.info("Searching accounts with keyword: {}", sanitizedKeyword);
                pageComptes = compteBancaireService.rechercherComptes(sanitizedKeyword, pageable);
                model.addAttribute("keyword", sanitizedKeyword);
            } else {
                logger.debug("Listing all accounts with pagination");
                pageComptes = compteBancaireService.listerTousLesComptes(pageable);
            }


            addPaginationAttributes(model, pageComptes);


            model.addAttribute("currentSort", sortBy);
            model.addAttribute("currentDirection", direction);
            model.addAttribute("oppositeDirection", "asc".equals(direction) ? "desc" : "asc");

            logger.debug("Successfully loaded {} accounts", pageComptes.getNumberOfElements());

        } catch (Exception e) {
            logger.error("Error loading accounts list", e);
            model.addAttribute(ERROR_MESSAGE, "Erreur lors du chargement de la liste des comptes");
        }

        return "comptes/liste-comptes";
    }


    private void addPaginationAttributes(Model model, Page<CompteBancaire> pageComptes) {
        model.addAttribute("pageComptes", pageComptes);

        int totalPages = pageComptes.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }


        model.addAttribute("currentPage", pageComptes.getNumber());
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", pageComptes.getTotalElements());
        model.addAttribute("hasNext", pageComptes.hasNext());
        model.addAttribute("hasPrevious", pageComptes.hasPrevious());
    }


    @GetMapping("/nouveau")
    public String afficherFormulaireAjout(Model model) {
        model.addAttribute("compteBancaire", new CompteBancaire());
        model.addAttribute("typesCompte", TypeCompte.values());
        model.addAttribute("pageTitle", "Ajouter un nouveau compte");
        return "comptes/formulaire-compte";
    }


    @PostMapping("/sauvegarder")
    public String sauvegarderCompte(
            @Valid @ModelAttribute("compteBancaire") CompteBancaire compte,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("typesCompte", TypeCompte.values());
            model.addAttribute("pageTitle",
                    compte.getId() == null ? "Ajouter un nouveau compte" : "Modifier le compte");
            return "comptes/formulaire-compte";
        }



        Optional<CompteBancaire> existingCompte = compteBancaireService
                .trouverCompteParNumero(compte.getNumeroCompte());
        if (existingCompte.isPresent()
                && (compte.getId() == null || !existingCompte.get().getId().equals(compte.getId()))) {
            result.rejectValue("numeroCompte", "error.compte", "Ce numéro de compte existe déjà.");
            model.addAttribute("typesCompte", TypeCompte.values());
            model.addAttribute("pageTitle",
                    compte.getId() == null ? "Ajouter un nouveau compte" : "Modifier le compte");
            return "comptes/formulaire-compte";
        }

        boolean isNew = compte.getId() == null;
        if (isNew) {
            compteBancaireService.creerCompte(compte);
            redirectAttributes.addFlashAttribute("successMessage", "Compte créé avec succès !");
        } else {
            compteBancaireService.modifierCompte(compte);
            redirectAttributes.addFlashAttribute("successMessage", "Compte modifié avec succès !");
        }
        return "redirect:/comptes";
    }


    @GetMapping("/modifier/{id}")
    public String afficherFormulaireModification(@PathVariable("id") Long id, Model model,
            RedirectAttributes redirectAttributes) {
        Optional<CompteBancaire> compteOpt = compteBancaireService.trouverCompteParId(id);
        if (compteOpt.isPresent()) {
            model.addAttribute("compteBancaire", compteOpt.get());
            model.addAttribute("typesCompte", TypeCompte.values());
            model.addAttribute("pageTitle", "Modifier le compte");
            return "comptes/formulaire-compte";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Compte non trouvé.");
            return "redirect:/comptes";
        }
    }


    @GetMapping("/supprimer/{id}")
    public String supprimerCompte(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            compteBancaireService.supprimerCompte(id);
            redirectAttributes.addFlashAttribute("successMessage", "Compte supprimé avec succès !");
        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erreur lors de la suppression du compte : " + e.getMessage());
        }
        return "redirect:/comptes";
    }


    @GetMapping("/{id}/depot")
    public String afficherFormulaireDepot(@PathVariable("id") Long id, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<CompteBancaire> compteOpt = compteBancaireService.trouverCompteParId(id);
            if (compteOpt.isPresent()) {
                model.addAttribute("compte", compteOpt.get());
                model.addAttribute("operationRequest", new OperationRequest());
                logger.debug("Displaying deposit form for account ID: {}", id);
                return "operations/depot";
            } else {
                logger.warn("Account not found for deposit form: {}", id);
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Compte non trouvé.");
                return "redirect:/comptes";
            }
        } catch (Exception e) {
            logger.error("Error displaying deposit form for account {}", id, e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Erreur lors de l'affichage du formulaire de dépôt");
            return "redirect:/comptes";
        }
    }


    @PostMapping("/{id}/depot")
    public String effectuerDepot(@PathVariable("id") Long id,
            @Valid @ModelAttribute("operationRequest") OperationRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            try {
                Optional<CompteBancaire> compteOpt = compteBancaireService.trouverCompteParId(id);
                if (compteOpt.isPresent()) {
                    model.addAttribute("compte", compteOpt.get());
                    return "operations/depot";
                }
            } catch (Exception e) {
                logger.error("Error reloading account for deposit form validation", e);
            }
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Données invalides");
            return "redirect:/comptes/" + id + "/depot";
        }

        try {
            compteBancaireService.effectuerDepot(id, request.getMontant());
            logger.info("Deposit successful for account {} - Amount: {}", id, request.getMontant());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                    String.format("Dépôt de %.2f € effectué avec succès !", request.getMontant()));
            return "redirect:/comptes/" + id + "/operations";
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Deposit failed for account {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessageDepot", e.getMessage());
            return "redirect:/comptes/" + id + "/depot";
        } catch (Exception e) {
            logger.error("Unexpected error during deposit for account {}", id, e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Erreur inattendue lors du dépôt");
            return "redirect:/comptes/" + id + "/depot";
        }
    }


    @GetMapping("/{id}/retrait")
    public String afficherFormulaireRetrait(@PathVariable("id") Long id, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<CompteBancaire> compteOpt = compteBancaireService.trouverCompteParId(id);
            if (compteOpt.isPresent()) {
                model.addAttribute("compte", compteOpt.get());
                model.addAttribute("operationRequest", new OperationRequest());
                logger.debug("Displaying withdrawal form for account ID: {}", id);
                return "operations/retrait";
            } else {
                logger.warn("Account not found for withdrawal form: {}", id);
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Compte non trouvé.");
                return "redirect:/comptes";
            }
        } catch (Exception e) {
            logger.error("Error displaying withdrawal form for account {}", id, e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Erreur lors de l'affichage du formulaire de retrait");
            return "redirect:/comptes";
        }
    }


    @PostMapping("/{id}/retrait")
    public String effectuerRetrait(@PathVariable("id") Long id,
            @Valid @ModelAttribute("operationRequest") OperationRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            try {
                Optional<CompteBancaire> compteOpt = compteBancaireService.trouverCompteParId(id);
                if (compteOpt.isPresent()) {
                    model.addAttribute("compte", compteOpt.get());
                    return "operations/retrait";
                }
            } catch (Exception e) {
                logger.error("Error reloading account for withdrawal form validation", e);
            }
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Données invalides");
            return "redirect:/comptes/" + id + "/retrait";
        }

        try {
            compteBancaireService.effectuerRetrait(id, request.getMontant());
            logger.info("Withdrawal successful for account {} - Amount: {}", id, request.getMontant());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                    String.format("Retrait de %.2f € effectué avec succès !", request.getMontant()));
            return "redirect:/comptes/" + id + "/operations";
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Withdrawal failed for account {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessageRetrait", e.getMessage());
            return "redirect:/comptes/" + id + "/retrait";
        } catch (Exception e) {
            logger.error("Unexpected error during withdrawal for account {}", id, e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Erreur inattendue lors du retrait");
            return "redirect:/comptes/" + id + "/retrait";
        }
    }


    @GetMapping("/virement")
    public String afficherFormulaireVirement(Model model, RedirectAttributes redirectAttributes) {
        try {
            List<CompteBancaire> comptes = compteBancaireService.listerTousLesComptesPourVirement();
            if (comptes.size() < 2) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                        "Au moins deux comptes sont nécessaires pour effectuer un virement.");
                return "redirect:/comptes";
            }
            model.addAttribute("comptes", comptes);
            model.addAttribute("virementRequest", new VirementRequest());
            logger.debug("Displaying transfer form with {} available accounts", comptes.size());
            return "operations/virement";
        } catch (Exception e) {
            logger.error("Error displaying transfer form", e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Erreur lors de l'affichage du formulaire de virement");
            return "redirect:/comptes";
        }
    }


    @PostMapping("/virement")
    public String effectuerVirement(@Valid @ModelAttribute("virementRequest") VirementRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            try {
                List<CompteBancaire> comptes = compteBancaireService.listerTousLesComptesPourVirement();
                model.addAttribute("comptes", comptes);
                return "operations/virement";
            } catch (Exception e) {
                logger.error("Error reloading accounts for transfer form validation", e);
            }
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Données invalides");
            return "redirect:/comptes/virement";
        }


        if (request.getIdCompteSource().equals(request.getIdCompteCible())) {
            redirectAttributes.addFlashAttribute("errorMessageVirement",
                    "Le compte source et le compte destination doivent être différents.");
            return "redirect:/comptes/virement";
        }

        try {
            compteBancaireService.effectuerVirement(
                    request.getIdCompteSource(),
                    request.getIdCompteCible(),
                    request.getMontant());
            logger.info("Transfer successful from account {} to account {} - Amount: {}",
                    request.getIdCompteSource(), request.getIdCompteCible(), request.getMontant());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                    String.format("Virement de %.2f € effectué avec succès !", request.getMontant()));
            return "redirect:/comptes";
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Transfer failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessageVirement", e.getMessage());
            return "redirect:/comptes/virement";
        } catch (Exception e) {
            logger.error("Unexpected error during transfer", e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Erreur inattendue lors du virement");
            return "redirect:/comptes/virement";
        }
    }


    @GetMapping("/{compteId}/operations")
    public String listerOperations(
            @PathVariable("compteId") Long compteId,
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            RedirectAttributes redirectAttributes) {

        Optional<CompteBancaire> compteOpt = compteBancaireService.trouverCompteParId(compteId);
        if (compteOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Compte non trouvé.");
            return "redirect:/comptes";
        }

        CompteBancaire compte = compteOpt.get();
        Pageable pageable = PageRequest.of(page, size);
        Page<Operation> pageOperations = compteBancaireService.listerOperationsParCompte(compteId, pageable);

        model.addAttribute("compte", compte);
        model.addAttribute("pageOperations", pageOperations);


        Map<String, Object> monthlyStats = compteBancaireService.obtenirStatistiquesMensuelles(compteId);
        model.addAttribute("monthlyDeposits", monthlyStats.get("monthlyDeposits"));
        model.addAttribute("monthlyWithdrawals", monthlyStats.get("monthlyWithdrawals"));

        int totalPages = pageOperations.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        return "comptes/operations";
    }




    @GetMapping("/api/comptes/{compteId}/chart-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAccountChartData(@PathVariable Long compteId) {
        try {
            Optional<CompteBancaire> compteOpt = compteBancaireService.trouverCompteParId(compteId);
            if (compteOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> chartData = compteBancaireService.obtenirDonneesGraphique(compteId);
            return ResponseEntity.ok(chartData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAccountStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAccounts", compteBancaireService.getTotalAccountCount());
            stats.put("totalBalance", compteBancaireService.obtenirSoldeTotalTousComptes());
            stats.put("averageBalance", compteBancaireService.obtenirSoldeMoyenComptes());
            stats.put("totalOperations", compteBancaireService.obtenirNombreTotalOperations());
            stats.put("accountsByType", compteBancaireService.obtenirRepartitionParTypeCompte());

            logger.debug("Account statistics retrieved successfully");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error retrieving account statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des statistiques"));
        }
    }

    @GetMapping("/api/accounts")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllAccountsForPDF() {
        try {
            List<CompteBancaire> comptes = compteBancaireService.listerTousLesComptesPourVirement();
            List<Map<String, Object>> accountsData = comptes.stream().map(compte -> {
                Map<String, Object> accountInfo = new HashMap<>();
                accountInfo.put("numeroCompte", compte.getNumeroCompte());
                accountInfo.put("nomTitulaire", compte.getNomTitulaire());
                accountInfo.put("solde", compte.getSolde());
                accountInfo.put("typeCompte", compte.getTypeCompte().toString());
                accountInfo.put("dateCreation", compte.getDateOuverture());
                return accountInfo;
            }).collect(Collectors.toList());
            
            logger.debug("Retrieved {} accounts for PDF", accountsData.size());
            return ResponseEntity.ok(accountsData);
        } catch (Exception e) {
            logger.error("Error retrieving accounts for PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAccountDetails(@PathVariable("id") Long id) {
        try {
            Optional<CompteBancaire> compteOpt = compteBancaireService.trouverCompteParId(id);
            if (compteOpt.isPresent()) {
                CompteBancaire compte = compteOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("id", compte.getId());
                response.put("numeroCompte", compte.getNumeroCompte());
                response.put("nomTitulaire", compte.getNomTitulaire());
                response.put("solde", compte.getSolde());
                response.put("typeCompte", compte.getTypeCompte());
                response.put("dateCreation", compte.getDateOuverture());

                logger.debug("Account details retrieved for ID: {}", id);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving account details for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des détails du compte"));
        }
    }


    @GetMapping("/api/validate-account-number")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateAccountNumber(
            @RequestParam("numeroCompte") String numeroCompte,
            @RequestParam(value = "excludeId", required = false) Long excludeId) {
        try {
            Optional<CompteBancaire> existingCompte = compteBancaireService.trouverCompteParNumero(numeroCompte);
            boolean isAvailable = !existingCompte.isPresent() ||
                    (excludeId != null && existingCompte.get().getId().equals(excludeId));

            Map<String, Object> response = new HashMap<>();
            response.put("available", isAvailable);
            response.put("message", isAvailable ? "Numéro de compte disponible" : "Ce numéro de compte existe déjà");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error validating account number: {}", numeroCompte, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la validation"));
        }
    }


    @GetMapping("/api/{id}/balance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAccountBalance(@PathVariable("id") Long id) {
        try {
            Optional<CompteBancaire> compteOpt = compteBancaireService.trouverCompteParId(id);
            if (compteOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("balance", compteOpt.get().getSolde());
                response.put("accountNumber", compteOpt.get().getNumeroCompte());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving balance for account ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération du solde"));
        }
    }


    @GetMapping("/api/stats/download")
    public ResponseEntity<String> downloadStats() {
        try {
            Map<String, Object> stats = compteBancaireService.obtenirStatistiques();
            
            StringBuilder csv = new StringBuilder();
            csv.append("Statistic,Value\n");
            csv.append("Total Accounts,").append(stats.get("nombreTotalComptes")).append("\n");
            csv.append("Total Balance,€").append(stats.get("soldeTotal")).append("\n");
            csv.append("Average Balance,€").append(stats.get("soldeMoyen")).append("\n");
            csv.append("Total Operations,").append(stats.get("nombreOperations")).append("\n");
            

            @SuppressWarnings("unchecked")
            Map<TypeCompte, Long> repartition = (Map<TypeCompte, Long>) stats.get("repartitionParType");
            if (repartition != null) {
                csv.append("\nAccount Type Distribution:\n");
                csv.append("Account Type,Count\n");
                for (Map.Entry<TypeCompte, Long> entry : repartition.entrySet()) {
                    csv.append(entry.getKey().getLibelle()).append(",").append(entry.getValue()).append("\n");
                }
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "bank_statistics.csv");
            
            logger.info("Statistics CSV downloaded successfully");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv.toString());
        } catch (Exception e) {
            logger.error("Error generating statistics CSV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating statistics file");
        }
    }


    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, RedirectAttributes redirectAttributes) {
        logger.error("Unhandled exception in CompteBancaireController", e);
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                "Une erreur inattendue s'est produite. Veuillez réessayer.");
        return "redirect:/comptes";
    }
}