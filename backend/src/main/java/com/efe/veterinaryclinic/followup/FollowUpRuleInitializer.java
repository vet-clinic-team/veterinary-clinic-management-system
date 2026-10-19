
package com.efe.veterinaryclinic.followup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FollowUpRuleInitializer implements CommandLineRunner {

    private final FollowUpRuleRepository ruleRepository;

    public FollowUpRuleInitializer(
            FollowUpRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        // Example protocols for development and testing only.
        // Replace intervals with veterinarian-approved rules.
        createRuleIfMissing(
                "CAT",
                "PARASITE_TREATMENT",
                "Cat Parasite Treatment",
                3,
                IntervalUnit.MONTH
        );

        createRuleIfMissing(
                "DOG",
                "PARASITE_TREATMENT",
                "Dog Parasite Treatment",
                3,
                IntervalUnit.MONTH
        );
    }

    private void createRuleIfMissing(
            String species,
            String procedureType,
            String name,
            int intervalValue,
            IntervalUnit intervalUnit) {

        boolean exists = ruleRepository
                .findFirstBySpeciesIgnoreCaseAndProcedureTypeIgnoreCaseAndActiveTrue(
                        species,
                        procedureType
                )
                .isPresent();

        if (exists) {
            return;
        }

        FollowUpRule rule = new FollowUpRule(
                species,
                procedureType,
                name
        );

        // First application schedules the next follow-up.
        rule.addStep(1, intervalValue, intervalUnit);

        // Continue scheduling after subsequent applications.
        rule.configureRepetition(
                intervalValue,
                intervalUnit
        );

        ruleRepository.save(rule);
    }
}

