
package com.efe.veterinaryclinic.treatment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TreatmentPlanRepository
        extends JpaRepository<TreatmentPlan, Long> {

    // Find all treatment plans belonging to a pet.
    List<TreatmentPlan> findByPet_IdOrderByCreatedAtDesc(
            Long petId
    );

    // Find the latest active treatment plan for a pet and procedure.
    Optional<TreatmentPlan>
    findFirstByPet_IdAndProcedureTypeIgnoreCaseAndStatusOrderByCreatedAtDesc(
            Long petId,
            String procedureType,
            TreatmentPlanStatus status
    );
}

