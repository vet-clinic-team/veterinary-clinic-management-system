
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.vaccination.Vaccination;
import com.efe.veterinaryclinic.vaccination.VaccinationRepository;
import com.efe.veterinaryclinic.catalog.ProcedureCatalogService;


import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;



@Service
public class FollowUpRuleService {

    private final FollowUpRuleRepository ruleRepository;
    private final FollowUpRuleStepRepository stepRepository;
    private final VaccinationRepository vaccinationRepository;
    private final ProcedureCatalogService catalogService;
    private final ApplicationEventPublisher eventPublisher;

    public FollowUpRuleService(
        FollowUpRuleRepository ruleRepository,
        FollowUpRuleStepRepository stepRepository,
        VaccinationRepository vaccinationRepository,
        ProcedureCatalogService catalogService,
        ApplicationEventPublisher eventPublisher) {

    this.ruleRepository = ruleRepository;
    this.stepRepository = stepRepository;
    this.vaccinationRepository = vaccinationRepository;
    this.catalogService = catalogService;
    this.eventPublisher = eventPublisher;
}

    // Calculate the next due date when creating a vaccination.
    // Previous vaccinations determine the current rule step.
    public Optional<LocalDate> calculateNextDueDate(
            Long petId,
            String species,
            String procedureType,
            LocalDate performedDate) {

        Optional<FollowUpRule> ruleOptional =
                findActiveRule(species, procedureType);

        if (ruleOptional.isEmpty()) {
            return Optional.empty();
        }

        long previousApplications =
                vaccinationRepository
                        .countByPet_IdAndVaccineTypeIgnoreCaseAndAdministeredAtBefore(
                                petId,
                                procedureType,
                                performedDate.atStartOfDay()
                        );

        return calculateNextDate(
                ruleOptional.get(),
                previousApplications,
                performedDate
        );
    }

    // Calculate the next due date using the exact vaccination
    // timestamp and a stable ID tie-breaker.
    public Optional<LocalDate> calculateNextDueDateForUpdate(
            Long vaccinationId,
            Long petId,
            String species,
            String procedureType,
            LocalDate performedDate) {

        Optional<FollowUpRule> ruleOptional =
                findActiveRule(species, procedureType);

        if (ruleOptional.isEmpty()) {
            return Optional.empty();
        }

        Vaccination currentVaccination =
                vaccinationRepository.findById(vaccinationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vaccination not found: " + vaccinationId
                                )
                        );

        LocalDateTime performedAt =
                currentVaccination.getAdministeredAt();

        List<Vaccination> vaccinations =
                vaccinationRepository
                        .findByPet_IdAndVaccineTypeIgnoreCaseOrderByAdministeredAtAscIdAsc(
                                petId,
                                procedureType
                        );

        long previousApplications = vaccinations.stream()
                .filter(vaccination ->
                        vaccination.getAdministeredAt().isBefore(performedAt)
                        || (
                                vaccination.getAdministeredAt().isEqual(performedAt)
                                && vaccination.getId() < vaccinationId
                        )
                )
                .count();

        return calculateNextDate(
                ruleOptional.get(),
                previousApplications,
                performedAt.toLocalDate()
        );
    }

    // Calculate a follow-up date for non-vaccination procedures.
    // The caller supplies the number of previous applications.
    // This method does not depend on vaccination history.
    public Optional<LocalDate> calculateNextDueDateForProcedure(
            String species,
            String procedureType,
            LocalDate performedDate,
            long previousApplications) {

        if (species == null
                || species.isBlank()
                || procedureType == null
                || procedureType.isBlank()
                || performedDate == null) {

            return Optional.empty();
        }

        if (previousApplications < 0) {
            throw new IllegalArgumentException(
                    "Previous applications cannot be negative"
            );
        }

        Optional<FollowUpRule> ruleOptional =
                findActiveRule(species, procedureType);

        if (ruleOptional.isEmpty()) {
            return Optional.empty();
        }

        return calculateNextDate(
                ruleOptional.get(),
                previousApplications,
                performedDate
        );
    }


    // Find a species-specific rule first, then an approved general rule.
    private Optional<FollowUpRule> findActiveRule(
            String species,
            String procedureType) {

        // First, search for a rule specific to the animal species.
        Optional<FollowUpRule> specificRule = ruleRepository
                .findFirstBySpeciesIgnoreCaseAndProcedureTypeIgnoreCaseAndActiveTrue(
                        species,
                        procedureType
                );

        if (specificRule.isPresent()) {
            return specificRule;
        }

        // Use a general rule only when one has been explicitly configured.
        return ruleRepository
                .findFirstBySpeciesIgnoreCaseAndProcedureTypeIgnoreCaseAndActiveTrue(
                        "ALL",
                        procedureType
                );
    }


    // Select the appropriate rule step or repeating interval.
    private Optional<LocalDate> calculateNextDate(
            FollowUpRule rule,
            long previousApplications,
            LocalDate performedDate) {

        List<FollowUpRuleStep> steps =
                stepRepository.findByRuleIdOrderByStepNumberAsc(
                        rule.getId()
                );

        long currentApplicationNumber = previousApplications + 1;

        Optional<FollowUpRuleStep> currentStep = steps.stream()
                .filter(step ->
                        step.getStepNumber() == currentApplicationNumber
                )
                .findFirst();

        if (currentStep.isPresent()) {

            FollowUpRuleStep step = currentStep.get();

            return Optional.of(
                    addInterval(
                            performedDate,
                            step.getIntervalValue(),
                            step.getIntervalUnit()
                    )
            );
        }

        long lastStepNumber = steps.stream()
                .mapToInt(FollowUpRuleStep::getStepNumber)
                .max()
                .orElse(0);

        if (currentApplicationNumber > lastStepNumber
                && rule.isRepeatAfterLastStep()) {

            Integer repeatValue = rule.getRepeatIntervalValue();
            IntervalUnit repeatUnit = rule.getRepeatIntervalUnit();

            if (repeatValue == null
                    || repeatValue < 1
                    || repeatUnit == null) {

                throw new IllegalStateException(
                        "Repeating follow-up rule has an invalid interval"
                );
            }

            return Optional.of(
                    addInterval(
                            performedDate,
                            repeatValue,
                            repeatUnit
                    )
            );
        }

        return Optional.empty();
    }

    // Add the configured interval to the procedure date.
    private LocalDate addInterval(
            LocalDate date,
            int intervalValue,
            IntervalUnit intervalUnit) {

        return switch (intervalUnit) {
            case DAY -> date.plusDays(intervalValue);
            case WEEK -> date.plusWeeks(intervalValue);
            case MONTH -> date.plusMonths(intervalValue);
            case YEAR -> date.plusYears(intervalValue);
        };
    }

@org.springframework.transaction.annotation.Transactional
public FollowUpRule createRule(
        String species,
        String procedureType,
        String name,
        List<RuleStepInput> steps,
        Integer repeatIntervalValue,
        IntervalUnit repeatIntervalUnit) {

    if (species == null || species.isBlank()
            || procedureType == null || procedureType.isBlank()
            || name == null || name.isBlank()) {
        throw new IllegalArgumentException(
                "Species, procedure type and name are required"
        );
    }

    String normalizedSpecies =
            species.trim().toUpperCase(java.util.Locale.ROOT);

    String normalizedProcedure =
            procedureType.trim().toUpperCase(java.util.Locale.ROOT);
            // Ensure the procedure exists and is active in the catalog.
catalogService.requireActive(
        normalizedProcedure,
        normalizedSpecies
);

    if (ruleRepository
            .existsBySpeciesIgnoreCaseAndProcedureTypeIgnoreCase(
                    normalizedSpecies,
                    normalizedProcedure)) {
        throw new IllegalArgumentException(
                "Follow-up rule already exists for this species and procedure"
        );
    }

    if ((repeatIntervalValue == null)
            != (repeatIntervalUnit == null)) {
        throw new IllegalArgumentException(
                "Repeat interval value and unit must be provided together"
        );
    }

    FollowUpRule rule = new FollowUpRule(
            normalizedSpecies,
            normalizedProcedure,
            name.trim()
    );

    if (steps != null) {
        for (RuleStepInput step : steps) {
            rule.addStep(
                    step.stepNumber(),
                    step.intervalValue(),
                    step.intervalUnit()
            );
        }
    }

    if (repeatIntervalValue != null) {
        rule.configureRepetition(
                repeatIntervalValue,
                repeatIntervalUnit
        );
    }

    if (rule.getSteps().isEmpty()
            && !rule.isRepeatAfterLastStep()) {
        throw new IllegalArgumentException(
                "At least one step or repeating interval is required"
        );
    }

    FollowUpRule savedRule = ruleRepository.saveAndFlush(rule);

eventPublisher.publishEvent(
        new FollowUpRuleCreatedEvent(
                normalizedSpecies,
                normalizedProcedure
        )
);

return savedRule;
}

@org.springframework.transaction.annotation.Transactional
public FollowUpRule updateRule(
        Long id,
        String name,
        boolean active,
        List<RuleStepInput> steps,
        Integer repeatIntervalValue,
        IntervalUnit repeatIntervalUnit) {

    FollowUpRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                    "Follow-up rule not found: " + id
            ));

    if ((repeatIntervalValue == null) != (repeatIntervalUnit == null)) {
        throw new IllegalArgumentException(
                "Repeat interval value and unit must be provided together"
        );
    }

    rule.updateDetails(name, active);
    rule.clearSteps();
    rule.disableRepetition();

    if (steps != null) {
        for (RuleStepInput step : steps) {
            rule.addStep(
                    step.stepNumber(),
                    step.intervalValue(),
                    step.intervalUnit()
            );
        }
    }

    if (repeatIntervalValue != null) {
        rule.configureRepetition(
                repeatIntervalValue,
                repeatIntervalUnit
        );
    }

    if (rule.getSteps().isEmpty() && !rule.isRepeatAfterLastStep()) {
        throw new IllegalArgumentException(
                "At least one step or repeating interval is required"
        );
    }

    FollowUpRule savedRule = ruleRepository.saveAndFlush(rule);

    if (savedRule.isActive()) {
    eventPublisher.publishEvent(
            new FollowUpRuleCreatedEvent(
                    savedRule.getSpecies(),
                    savedRule.getProcedureType()
            )
    );
}

    return savedRule;
}


public record RuleStepInput(
        int stepNumber,
        int intervalValue,
        IntervalUnit intervalUnit
) {}
// List follow-up rules for the selected species.
public List<FollowUpRule> getRulesBySpecies(String species) {
    return ruleRepository.findBySpeciesIgnoreCaseOrderByIdDesc(species);
}

}



