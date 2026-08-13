package com.efe.veterinaryclinic.pet;

import com.efe.veterinaryclinic.common.dto.PageResponse;
import com.efe.veterinaryclinic.invoice.InvoiceService;
import com.efe.veterinaryclinic.invoice.dto.InvoiceResponse;
import com.efe.veterinaryclinic.pet.dto.PetRequest;
import com.efe.veterinaryclinic.pet.dto.PetResponse;
import com.efe.veterinaryclinic.pet.dto.PetStatsResponse;
import com.efe.veterinaryclinic.pet.dto.PetWeightRecordRequest;
import com.efe.veterinaryclinic.pet.dto.PetWeightRecordResponse;
import com.efe.veterinaryclinic.vaccination.VaccinationService;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationResponse;
import com.efe.veterinaryclinic.visit.VisitService;
import com.efe.veterinaryclinic.visit.dto.VisitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@Tag(name = "Pets", description = "Pet records and their visit/vaccination/weight/invoice history")
public class PetController {


    private final PetService petService;
    private final VisitService visitService;
    private final VaccinationService vaccinationService;
    private final PetWeightRecordService petWeightRecordService;
    private final InvoiceService invoiceService;

    public PetController(PetService petService, VisitService visitService, VaccinationService vaccinationService,
                          PetWeightRecordService petWeightRecordService, InvoiceService invoiceService) {
        this.petService = petService;
        this.visitService = visitService;
        this.vaccinationService = vaccinationService;
        this.petWeightRecordService = petWeightRecordService;
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @Operation(summary = "Create a pet", description = "ADMIN, RECEPTIONIST. Breed is optional (speciesNote required instead) unless species is CAT or DOG.")
    public ResponseEntity<PetResponse> create(@Valid @RequestBody PetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.create(request));
    }

    @GetMapping
    @Operation(summary = "List pets", description = "ADMIN, VET, RECEPTIONIST. Supports search, species/owner/active filters, sort, and pagination.")
    public ResponseEntity<PageResponse<PetResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(petService.list(search, species, ownerId, active, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pet detail", description = "ADMIN, VET, RECEPTIONIST.")
    public ResponseEntity<PetResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(petService.getById(id));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get pet statistics", description = "ADMIN, VET, RECEPTIONIST. Totals for dashboard stat cards, computed over the full dataset (not just the current page). totalPets/dogs/cats count only non-archived pets.")
    public ResponseEntity<PetStatsResponse> getStats() {
        return ResponseEntity.ok(petService.getStats());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a pet", description = "ADMIN, RECEPTIONIST.")
    public ResponseEntity<PetResponse> update(@PathVariable Long id, @Valid @RequestBody PetRequest request) {
        return ResponseEntity.ok(petService.update(id, request));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive a pet", description = "ADMIN, RECEPTIONIST. Soft delete — pets are never hard-deleted.")
    public ResponseEntity<PetResponse> archive(@PathVariable Long id) {
        return ResponseEntity.ok(petService.archive(id));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Reactivate an archived pet", description = "ADMIN, RECEPTIONIST.")
    public ResponseEntity<PetResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(petService.activate(id));
    }

    @GetMapping("/{id}/visits")
    @Operation(summary = "Get a pet's visit history", description = "ADMIN, VET, RECEPTIONIST.")
    public ResponseEntity<PageResponse<VisitResponse>> visits(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(visitService.listByPet(id, pageable));
    }

    @GetMapping("/{id}/vaccinations")
    @Operation(summary = "Get a pet's vaccination history", description = "ADMIN, VET, RECEPTIONIST.")
    public ResponseEntity<PageResponse<VaccinationResponse>> vaccinations(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(vaccinationService.listByPet(id, pageable));
    }

    @GetMapping("/{id}/invoices")
    @Operation(summary = "Get a pet's invoice history", description = "ADMIN, VET, RECEPTIONIST.")
    public ResponseEntity<PageResponse<InvoiceResponse>> invoices(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(invoiceService.listByPet(id, pageable));
    }

    @PostMapping("/{id}/weight-records")
    @Operation(summary = "Add a weight record", description = "ADMIN, VET, RECEPTIONIST. Weighing can happen at check-in.")
    public ResponseEntity<PetWeightRecordResponse> addWeightRecord(@PathVariable Long id,
                                                                     @Valid @RequestBody PetWeightRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petWeightRecordService.create(id, request));
    }

    @GetMapping("/{id}/weight-records")
    @Operation(summary = "Get a pet's weight history", description = "ADMIN, VET, RECEPTIONIST. Returns a plain array (not paginated) ordered by recordedAt ascending.")
    public ResponseEntity<List<PetWeightRecordResponse>> weightRecords(@PathVariable Long id) {
        return ResponseEntity.ok(petWeightRecordService.listByPet(id));
    }
}
