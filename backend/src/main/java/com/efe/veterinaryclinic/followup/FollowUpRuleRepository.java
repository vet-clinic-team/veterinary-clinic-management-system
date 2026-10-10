package com.efe.veterinaryclinic.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowUpRuleRepository
        extends JpaRepository<FollowUpRule, Long> {

    Optional<FollowUpRule> findFirstBySpeciesIgnoreCaseAndProcedureTypeIgnoreCaseAndActiveTrue(
            String species,
            String procedureType
    );
}