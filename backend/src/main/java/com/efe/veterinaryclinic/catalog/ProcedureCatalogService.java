
package com.efe.veterinaryclinic.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ProcedureCatalogService {

    private final ProcedureCatalogRepository catalogRepository;

    public ProcedureCatalogService(
            ProcedureCatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    // List active procedures for a species.
    @Transactional(readOnly = true)
    public List<ProcedureCatalog> listBySpecies(String species) {

        String normalizedSpecies = normalize(species);

        return catalogRepository
                .findBySpeciesIgnoreCaseAndActiveTrue(
                        normalizedSpecies
                );
    }

    // Register a new vaccine or treatment.
    @Transactional
    public ProcedureCatalog create(
            String code,
            String name,
            String species,
            String category) {

        String normalizedCode = normalize(code);
        String normalizedSpecies = normalize(species);
        String normalizedCategory = normalize(category);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Procedure name is required"
            );
        }

        if (normalizedCode.length() > 100
                || name.trim().length() > 150
                || normalizedSpecies.length() > 100
                || normalizedCategory.length() > 50) {
            throw new IllegalArgumentException(
                    "Procedure field exceeds maximum length"
            );
        }

        if (catalogRepository
                .findByCodeIgnoreCaseAndSpeciesIgnoreCase(
                        normalizedCode,
                        normalizedSpecies
                )
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Procedure already exists for this species"
            );
        }

        ProcedureCatalog procedure = new ProcedureCatalog(
                normalizedCode,
                name.trim(),
                normalizedSpecies,
                normalizedCategory
        );

        return catalogRepository.save(procedure);
    }

    // Verify that a procedure exists and is active.
    @Transactional(readOnly = true)
    public ProcedureCatalog requireActive(
            String code,
            String species) {

        return catalogRepository
                .findByCodeIgnoreCaseAndSpeciesIgnoreCase(
                        normalize(code),
                        normalize(species)
                )
                .filter(ProcedureCatalog::isActive)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Active procedure not found for this species"
                        )
                );
    }

    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required catalog field is missing"
            );
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }
}

