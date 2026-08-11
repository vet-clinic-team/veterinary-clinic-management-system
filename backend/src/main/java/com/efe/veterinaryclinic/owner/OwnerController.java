package com.efe.veterinaryclinic.owner;

import com.efe.veterinaryclinic.common.dto.PageResponse;
import com.efe.veterinaryclinic.owner.dto.OwnerDetailResponse;
import com.efe.veterinaryclinic.owner.dto.OwnerRequest;
import com.efe.veterinaryclinic.owner.dto.OwnerResponse;
import com.efe.veterinaryclinic.owner.dto.OwnerStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owners")
@Tag(name = "Owners", description = "Pet owner records")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @PostMapping
    @Operation(summary = "Create an owner", description = "ADMIN, RECEPTIONIST.")
    public ResponseEntity<OwnerResponse> create(@Valid @RequestBody OwnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerService.create(request));
    }

    @GetMapping
    @Operation(summary = "List owners", description = "ADMIN, VET, RECEPTIONIST. Supports name search, active/archived filter, and pagination.")
    public ResponseEntity<PageResponse<OwnerResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(ownerService.list(search, active, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get owner detail", description = "ADMIN, VET, RECEPTIONIST. Includes the owner's pets and pet count.")
    public ResponseEntity<OwnerDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ownerService.getById(id));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get owner statistics", description = "ADMIN, VET, RECEPTIONIST. Totals for dashboard stat cards, computed over the full dataset (not just the current page).")
    public ResponseEntity<OwnerStatsResponse> getStats() {
        return ResponseEntity.ok(ownerService.getStats());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an owner", description = "ADMIN, RECEPTIONIST.")
    public ResponseEntity<OwnerResponse> update(@PathVariable Long id, @Valid @RequestBody OwnerRequest request) {
        return ResponseEntity.ok(ownerService.update(id, request));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive an owner", description = "ADMIN, RECEPTIONIST. Soft delete — rejected with 409 if the owner still has active (non-archived) pets.")
    public ResponseEntity<OwnerResponse> archive(@PathVariable Long id) {
        return ResponseEntity.ok(ownerService.archive(id));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Reactivate an archived owner", description = "ADMIN, RECEPTIONIST.")
    public ResponseEntity<OwnerResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ownerService.activate(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an owner", description = "ADMIN only. Owner must already be archived (409 otherwise), and rejected with 409 if the owner still has pets.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ownerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
