
package com.efe.veterinaryclinic.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface FollowUpRuleRepository
        extends JpaRepository<FollowUpRule, Long> {

    // Find an active rule by species and procedure type.
    Optional<FollowUpRule> findFirstBySpeciesIgnoreCaseAndProcedureTypeIgnoreCaseAndActiveTrue(
            String species,
            String procedureType
    );

    // Check whether a rule already exists for this species and procedure.
    boolean existsBySpeciesIgnoreCaseAndProcedureTypeIgnoreCase(
            String species,
            String procedureType
    );
    // List all rules for a selected animal species.
List<FollowUpRule> findBySpeciesIgnoreCaseOrderByIdDesc(
        String species
);
}
