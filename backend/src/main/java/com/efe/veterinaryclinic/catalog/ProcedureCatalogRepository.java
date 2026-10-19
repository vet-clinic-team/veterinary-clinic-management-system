
package com.efe.veterinaryclinic.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedureCatalogRepository
        extends JpaRepository<ProcedureCatalog, Long> {

    List<ProcedureCatalog> findBySpeciesIgnoreCaseAndActiveTrue(
            String species
    );

    Optional<ProcedureCatalog> findByCodeIgnoreCaseAndSpeciesIgnoreCase(
            String code,
            String species
    );
}

