
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.followup.dto.FollowUpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/follow-ups")
@Tag(
        name = "Follow-ups",
        description = "Automatic and manual follow-up tracking"
)
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    @GetMapping("/pet/{petId}")
    @Operation(
            summary = "List follow-ups for a pet",
            description = "ADMIN, VET, RECEPTIONIST."
    )
    public ResponseEntity<List<FollowUpResponse>> getByPet(
            @PathVariable Long petId) {

        List<FollowUpResponse> response = followUpService
                .getFollowUpsByPet(petId)
                .stream()
                .map(FollowUpResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    @Operation(
            summary = "List pending follow-ups",
            description = "ADMIN, VET, RECEPTIONIST."
    )
    public ResponseEntity<List<FollowUpResponse>> getPending() {

        List<FollowUpResponse> response = followUpService
                .getPendingFollowUps()
                .stream()
                .map(FollowUpResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    // Request body for manually changing the follow-up date.
    public record RescheduleRequest(LocalDate dueDate) {
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(
            summary = "Manually reschedule a follow-up",
            description = "ADMIN, VET only."
    )
    public ResponseEntity<FollowUpResponse> reschedule(
            @PathVariable Long id,
            @RequestBody RescheduleRequest request) {

        FollowUp followUp = followUpService.rescheduleManually(
                id,
                request.dueDate()
        );

        return ResponseEntity.ok(
                FollowUpResponse.from(followUp)
        );
    }

    @PatchMapping("/{id}/complete")
    @Operation(
            summary = "Manually complete a follow-up",
            description = "ADMIN, VET only."
    )
    public ResponseEntity<FollowUpResponse> completeManually(
            @PathVariable Long id) {

        FollowUp followUp = followUpService.completeManually(id);

        return ResponseEntity.ok(
                FollowUpResponse.from(followUp)
        );
    }
}


