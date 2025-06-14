package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Operation;
import com.example.demo.model.TypeOperation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {


        Page<Operation> findByCompteBancaireIdOrderByDateOperationDesc(Long compteId, Pageable pageable);


        List<Operation> findByCompteBancaireIdOrderByDateOperationDesc(Long compteId);


        long countByDateOperationBetween(LocalDateTime startDate, LocalDateTime endDate);


        List<Operation> findByCompteBancaireIdAndDateOperationBetweenOrderByDateOperationDesc(
                        Long compteId, LocalDateTime startDate, LocalDateTime endDate);


        List<Operation> findByDateOperationBetween(LocalDateTime startDate, LocalDateTime endDate);


        List<Operation> findByCompteBancaireIdOrderByDateOperationAsc(Long compteId);


        long countByCompteBancaireIdAndTypeOperationAndDateOperationBetween(
                        Long compteId, TypeOperation typeOperation, LocalDateTime startDate, LocalDateTime endDate);


        @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o WHERE o.compteBancaire.id = :compteId AND o.typeOperation = :typeOperation AND o.dateOperation BETWEEN :startDate AND :endDate")
        BigDecimal sumMontantByCompteBancaireIdAndTypeOperationAndDateOperationBetween(
                        @Param("compteId") Long compteId,
                        @Param("typeOperation") TypeOperation typeOperation,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);
}
