package com.efe.veterinaryclinic.visit;

import com.efe.veterinaryclinic.common.dto.PageResponse;
import com.efe.veterinaryclinic.security.CustomUserDetails;
import com.efe.veterinaryclinic.visit.dto.MedicalNotesUpdateRequest;
import com.efe.veterinaryclinic.visit.dto.VisitRequest;
import com.efe.veterinaryclinic.visit.dto.VisitResponse;
import com.efe.veterinaryclinic.visit.dto.VisitStatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/visits")
@Tag(name = "Visits", description = "Appointments, examinations, and treatment notes")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @PostMapping
    @Operation(summary = "Create a visit", description = "ADMIN, RECEPTIONIST. Rejected with 409 if the vet has an overlapping appointment within +/-15 minutes.")
    public ResponseEntity<VisitResponse> create(@Valid @RequestBody VisitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.create(request));
    }

    @GetMapping
    @Operation(summary = "List visits", description = "ADMIN, VET, RECEPTIONIST. Supports vet/date-range/status filters and pagination.")
    public ResponseEntity<PageResponse<VisitResponse>> list(
            @RequestParam(required = false) Long vetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) VisitStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(visitService.list(vetId, from, to, status, pageable));
    }

    @GetMapping("/calendar")
    @Operation(summary = "Get calendar view data", description = "ADMIN, VET, RECEPTIONIST. Same filters as list, but returns a plain array (not paginated) sorted by scheduledAt ascending — meant for a bounded from/to range.")
    public ResponseEntity<List<VisitResponse>> calendar(
            @RequestParam(required = false) Long vetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) VisitStatus status) {
        return ResponseEntity.ok(visitService.calendar(vetId, from, to, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get visit detail", description = "ADMIN, VET, RECEPTIONIST.")
    public ResponseEntity<VisitResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(visitService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a visit", description = "ADMIN, RECEPTIONIST.")
    public ResponseEntity<VisitResponse> update(@PathVariable Long id, @Valid @RequestBody VisitRequest request) {
        return ResponseEntity.ok(visitService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update visit status", description = "ADMIN, VET, RECEPTIONIST.")
    public ResponseEntity<VisitResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody VisitStatusUpdateRequest request) {
        return ResponseEntity.ok(visitService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/medical-notes")
    @Operation(summary = "Update diagnosis/treatment notes", description = "ADMIN, VET. RECEPTIONIST is forbidden from editing medical data. Returns a non-blocking allergy warning if a recorded allergy matches the treatment notes.")
    public ResponseEntity<VisitResponse> updateMedicalNotes(@PathVariable Long id,
                                                             @Valid @RequestBody MedicalNotesUpdateRequest request,
                                                             @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(visitService.updateMedicalNotes(id, request, principal.getUser().getRole()));
    }

    @PostMapping("/{id}/follow-up")
    @Operation(summary = "Create a follow-up visit", description = "ADMIN, VET. Source visit must be COMPLETED with a followUpDate set, otherwise 409.")
    public ResponseEntity<VisitResponse> createFollowUp(@PathVariable Long id,
                                                         @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(visitService.createFollowUp(id, principal.getUser().getRole()));
    }
}
