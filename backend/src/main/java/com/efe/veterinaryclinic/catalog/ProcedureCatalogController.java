
package com.efe.veterinaryclinic.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/procedures")
public class ProcedureCatalogController {

    private final ProcedureCatalogService catalogService;

    public ProcedureCatalogController(
            ProcedureCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // List active procedures for the selected species.
    @GetMapping
    public List<ProcedureCatalog> list(
            @RequestParam String species) {

        return catalogService.listBySpecies(species);
    }

    // Create a new vaccine or treatment.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProcedureCatalog create(
            @RequestBody CreateProcedureRequest request) {

        return catalogService.create(
                request.code(),
                request.name(),
                request.species(),
                request.category()
        );
    }

    public record CreateProcedureRequest(
            String code,
            String name,
            String species,
            String category
    ) {
    }
}

