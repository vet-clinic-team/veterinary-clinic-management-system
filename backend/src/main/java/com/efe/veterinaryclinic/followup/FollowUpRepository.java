
package com.efe.veterinaryclinic.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    List<FollowUp> findByPet_IdOrderByDueDateAsc(Long petId);

    List<FollowUp> findByStatusOrderByDueDateAsc(FollowUpStatus status);

    List<FollowUp> findByStatusAndDueDateLessThanEqualOrderByDueDateAsc(
            FollowUpStatus status,
            LocalDate dueDate
    );

    Optional<FollowUp> findBySourceTypeAndSourceRecordIdAndStatus(
            String sourceType,
            Long sourceRecordId,
            FollowUpStatus status
    );

    // Find all follow-ups associated with a source record.
    // Include completed and cancelled records to prevent duplicates.
    List<FollowUp> findBySourceTypeAndSourceRecordIdOrderByIdAsc(
            String sourceType,
            Long sourceRecordId
    );

    // Find pending follow-ups for the same pet and procedure.
    // The source type prevents matching unrelated clinical procedures.
    List<FollowUp>
    findByPet_IdAndProcedureTypeIgnoreCaseAndSourceTypeAndStatusOrderByDueDateAsc(
            Long petId,
            String procedureType,
            String sourceType,
            FollowUpStatus status
    );
}
