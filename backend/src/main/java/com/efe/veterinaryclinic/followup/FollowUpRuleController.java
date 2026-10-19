
package com.efe.veterinaryclinic.followup;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follow-up-rules")
public class FollowUpRuleController {

    private final FollowUpRuleService ruleService;

    public FollowUpRuleController(FollowUpRuleService ruleService) {
        this.ruleService = ruleService;
    }
    @GetMapping
@PreAuthorize("hasRole('ADMIN')")
public List<FollowUpRule> list(
        @RequestParam String species) {

    return ruleService.getRulesBySpecies(species);
}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public FollowUpRule create(
            @RequestBody CreateRuleRequest request) {

        return ruleService.createRule(
                request.species(),
                request.procedureType(),
                request.name(),
                request.steps(),
                request.repeatIntervalValue(),
                request.repeatIntervalUnit()
        );
    }

    public record CreateRuleRequest(
            String species,
            String procedureType,
            String name,
            List<FollowUpRuleService.RuleStepInput> steps,
            Integer repeatIntervalValue,
            IntervalUnit repeatIntervalUnit
    ) {}
    
@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public FollowUpRule update(
        @PathVariable Long id,
        @RequestBody UpdateRuleRequest request) {

    return ruleService.updateRule(
            id,
            request.name(),
            request.active(),
            request.steps(),
            request.repeatIntervalValue(),
            request.repeatIntervalUnit()
    );
}

public record UpdateRuleRequest(
        String name,
        boolean active,
        List<FollowUpRuleService.RuleStepInput> steps,
        Integer repeatIntervalValue,
        IntervalUnit repeatIntervalUnit
) {}

}

