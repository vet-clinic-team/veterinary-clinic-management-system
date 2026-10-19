
package com.efe.veterinaryclinic.catalog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProcedureCatalogInitializer implements CommandLineRunner {

    private final ProcedureCatalogRepository catalogRepository;

    public ProcedureCatalogInitializer(
            ProcedureCatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        // Catalog entries only. No medical schedules are defined here.
        createIfMissing("RABIES", "Rabies Vaccine", "CAT", "VACCINATION");
        createIfMissing("RABIES", "Rabies Vaccine", "DOG", "VACCINATION");

        createIfMissing(
                "PARASITE_TREATMENT",
                "Parasite Treatment",
                "CAT",
                "PARASITE_TREATMENT"
        );

        createIfMissing(
                "PARASITE_TREATMENT",
                "Parasite Treatment",
                "DOG",
                "PARASITE_TREATMENT"
        );
    }

    private void createIfMissing(
            String code,
            String name,
            String species,
            String category) {

        boolean exists = catalogRepository
                .findByCodeIgnoreCaseAndSpeciesIgnoreCase(code, species)
                .isPresent();

        if (exists) {
            return;
        }

        ProcedureCatalog procedure = new ProcedureCatalog(
                code,
                name,
                species,
                category
        );

        catalogRepository.save(procedure);
    }
}

