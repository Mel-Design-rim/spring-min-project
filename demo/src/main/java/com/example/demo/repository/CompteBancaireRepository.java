package com.example.demo.repository;
 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.CompteBancaire;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteBancaireRepository extends JpaRepository<CompteBancaire, Long> {

    Optional<CompteBancaire> findByNumeroCompte(String numeroCompte);


    Page<CompteBancaire> findByNomTitulaireContainingIgnoreCaseOrNumeroCompteContainingIgnoreCase(
            String nomTitulaire, String numeroCompte, Pageable pageable);


    List<CompteBancaire> findByNomTitulaireContainingIgnoreCaseOrNumeroCompteContainingIgnoreCase(
            String nomTitulaire, String numeroCompte);

}
