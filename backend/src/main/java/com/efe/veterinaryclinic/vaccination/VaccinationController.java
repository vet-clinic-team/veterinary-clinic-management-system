package com.efe.veterinaryclinic.vaccination;

import com.efe.veterinaryclinic.common.dto.PageResponse;
import com.efe.veterinaryclinic.security.CustomUserDetails;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationRequest;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationResponse;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vaccinations")
@Tag(name = "Vaccinations", description = "Vaccination records")
public class VaccinationController {

    private final VaccinationService vaccinationService;

    public VaccinationController(VaccinationService vaccinationService) {
        this.vaccinationService = vaccinationService;
    }

    @PostMapping
    @Operation(summary = "Create a vaccination record", description = "ADMIN, VET. nextDueDate is calculated by the backend.")
    public ResponseEntity<VaccinationResponse> create(@Valid @RequestBody VaccinationRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vaccinationService.create(request, principal.getUser().getRole()));
    }

    @GetMapping
    @Operation(summary = "List vaccinations", description = "ADMIN, VET, RECEPTIONIST.")
    public ResponseEntity<PageResponse<VaccinationResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(vaccinationService.list(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vaccination detail", description = "ADMIN, VET, RECEPTIONIST.")
    public ResponseEntity<VaccinationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vaccinationService.getById(id));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get vaccination statistics", description = "ADMIN, VET, RECEPTIONIST. Totals for dashboard stat cards, computed over the full dataset (not just the current page).")
    public ResponseEntity<VaccinationStatsResponse> getStats() {
        return ResponseEntity.ok(vaccinationService.getStats());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a vaccination record", description = "ADMIN, VET.")
    public ResponseEntity<VaccinationResponse> update(@PathVariable Long id, @Valid @RequestBody VaccinationRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(vaccinationService.update(id, request, principal.getUser().getRole()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a vaccination record", description = "ADMIN, VET.")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        vaccinationService.delete(id, principal.getUser().getRole());
        return ResponseEntity.noContent().build();
    }
}
