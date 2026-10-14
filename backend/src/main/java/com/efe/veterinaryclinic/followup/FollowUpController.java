
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.followup.dto.FollowUpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/follow-ups")
@Tag(name = "Follow-ups", description = "Automatic and manual follow-up tracking")
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

    @PatchMapping("/{id}/complete")
    @Operation(
            summary = "Manually complete a follow-up",
            description = "ADMIN, VET only."
    )
    public ResponseEntity<FollowUpResponse> completeManually(
            @PathVariable Long id) {

        FollowUp followUp = followUpService.completeManually(id);

        return ResponseEntity.ok(FollowUpResponse.from(followUp));
    }
}

