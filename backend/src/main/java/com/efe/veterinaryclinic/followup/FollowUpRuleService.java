
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.vaccination.Vaccination;
import com.efe.veterinaryclinic.vaccination.VaccinationRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FollowUpRuleService {

    private final FollowUpRuleRepository ruleRepository;
    private final FollowUpRuleStepRepository stepRepository;
    private final VaccinationRepository vaccinationRepository;

    public FollowUpRuleService(
            FollowUpRuleRepository ruleRepository,
            FollowUpRuleStepRepository stepRepository,
            VaccinationRepository vaccinationRepository) {

        this.ruleRepository = ruleRepository;
        this.stepRepository = stepRepository;
        this.vaccinationRepository = vaccinationRepository;
    }

    // Calculate the next due date when creating a vaccination.
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

    private Optional<FollowUpRule> findActiveRule(
            String species,
            String procedureType) {

        return ruleRepository
                .findFirstBySpeciesIgnoreCaseAndProcedureTypeIgnoreCaseAndActiveTrue(
                        species,
                        procedureType
                );
    }

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
}


