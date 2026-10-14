
package com.efe.veterinaryclinic.treatment;

import com.efe.veterinaryclinic.treatment.dto.TreatmentPlanRequest;
import com.efe.veterinaryclinic.treatment.dto.TreatmentPlanResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/treatment-plans")
public class TreatmentPlanController {

    private final TreatmentPlanService treatmentPlanService;

    public TreatmentPlanController(
            TreatmentPlanService treatmentPlanService) {

        this.treatmentPlanService = treatmentPlanService;
    }

    // Start a new treatment course for a pet.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'VET')")
    public TreatmentPlanResponse create(
            @Valid @RequestBody TreatmentPlanRequest request) {

        TreatmentPlan plan = treatmentPlanService.createPlan(
                request.petId(),
                request.procedureType()
        );

        // Return the actual number of completed applications.
        return treatmentPlanService.toResponse(plan);
    }

    // List the treatment courses belonging to a pet.
    @GetMapping("/pet/{petId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VET', 'RECEPTIONIST')")
    public List<TreatmentPlanResponse> listByPet(
            @PathVariable Long petId) {

        // Calculate completed applications for each treatment course.
        return treatmentPlanService.getPlansByPet(petId)
                .stream()
                .map(treatmentPlanService::toResponse)
                .toList();
    }
}


