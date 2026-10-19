
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.common.exception.ConflictException;
import com.efe.veterinaryclinic.common.exception.ResourceNotFoundException;
import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.vaccination.Vaccination;
import com.efe.veterinaryclinic.vaccination.VaccinationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import com.efe.veterinaryclinic.visit.Visit;
import com.efe.veterinaryclinic.visit.VisitStatus;

@Service
public class FollowUpService {

    private static final String VACCINATION_SOURCE = "VACCINATION";
    private static final String VISIT_SOURCE = "VISIT";

    private final FollowUpRepository followUpRepository;
    private final VaccinationRepository vaccinationRepository;

    public FollowUpService(
            FollowUpRepository followUpRepository,
            VaccinationRepository vaccinationRepository) {

        this.followUpRepository = followUpRepository;
        this.vaccinationRepository = vaccinationRepository;
    }

    @Transactional
    public FollowUp createOrUpdateFromVaccination(
            Pet pet,
            String procedureType,
            LocalDate dueDate,
            Long vaccinationId) {

        List<FollowUp> existingFollowUps = followUpRepository
                .findBySourceTypeAndSourceRecordIdOrderByIdAsc(
                        VACCINATION_SOURCE,
                        vaccinationId
                );

        // Update an existing pending follow-up.
        var pendingFollowUp = existingFollowUps.stream()
                .filter(followUp ->
                        followUp.getStatus() == FollowUpStatus.PENDING)
                .findFirst();

        if (pendingFollowUp.isPresent()) {

            FollowUp followUp = pendingFollowUp.get();

            followUp.updateDetails(
                    pet,
                    procedureType,
                    dueDate
            );

            return followUp;
        }

        // Preserve completed records and prevent duplicates.
        var completedFollowUp = existingFollowUps.stream()
                .filter(followUp ->
                        followUp.getStatus() == FollowUpStatus.COMPLETED)
                .findFirst();

        if (completedFollowUp.isPresent()) {
            return completedFollowUp.get();
        }

        // Keep cancelled records as history.
        FollowUp newFollowUp = new FollowUp(
                pet,
                procedureType,
                dueDate,
                VACCINATION_SOURCE,
                vaccinationId
        );

        return followUpRepository.save(newFollowUp);
    }

    @Transactional
    public void cancelFromVaccination(Long vaccinationId) {

        followUpRepository
                .findBySourceTypeAndSourceRecordIdAndStatus(
                        VACCINATION_SOURCE,
                        vaccinationId,
                        FollowUpStatus.PENDING
                )
                .ifPresent(FollowUp::cancel);
    }

    @Transactional
    public void syncFromVaccination(
            Pet pet,
            String procedureType,
            LocalDate dueDate,
            Long vaccinationId) {

        if (dueDate == null) {
            cancelFromVaccination(vaccinationId);
            return;
        }

        createOrUpdateFromVaccination(
                pet,
                procedureType,
                dueDate,
                vaccinationId
        );
    }

    @Transactional
    public void completePreviousVaccinationFollowUp(
            Long petId,
            String procedureType,
            LocalDateTime administeredAt) {

        List<FollowUp> pendingFollowUps = followUpRepository
                .findByPet_IdAndProcedureTypeIgnoreCaseAndSourceTypeAndStatusOrderByDueDateAsc(
                        petId,
                        procedureType,
                        VACCINATION_SOURCE,
                        FollowUpStatus.PENDING
                );

        pendingFollowUps.stream()
                .filter(followUp ->
                        !followUp.getDueDate()
                                .isAfter(administeredAt.toLocalDate()))
                .filter(followUp ->
                        vaccinationRepository
                                .findById(followUp.getSourceRecordId())
                                .filter(source ->
                                        Objects.equals(
                                                source.getPet().getId(),
                                                petId
                                        ))
                                .filter(source ->
                                        source.getVaccineType()
                                                .equalsIgnoreCase(procedureType))
                                .filter(source ->
                                        source.getAdministeredAt()
                                                .isBefore(administeredAt))
                                .isPresent())
                .min(Comparator.comparing(FollowUp::getDueDate))
                .ifPresent(FollowUp::markAutomaticallyCompleted);
    }

    @Transactional
    public void reconcileVaccinationFollowUps(
            Long petId,
            String procedureType) {

        // Retrieve vaccinations in chronological order.
        List<Vaccination> vaccinations = vaccinationRepository
                .findByPet_IdAndVaccineTypeIgnoreCaseOrderByAdministeredAtAscIdAsc(
                        petId,
                        procedureType
                );

        for (int i = 0; i < vaccinations.size(); i++) {

            Vaccination currentVaccination = vaccinations.get(i);

            List<FollowUp> followUps = followUpRepository
                    .findBySourceTypeAndSourceRecordIdOrderByIdAsc(
                            VACCINATION_SOURCE,
                            currentVaccination.getId()
                    );

            for (FollowUp followUp : followUps) {

                // Never modify cancelled follow-ups.
                if (followUp.getStatus() == FollowUpStatus.CANCELLED) {
                    continue;
                }

                // Preserve manually completed follow-ups.
                if (followUp.getStatus() == FollowUpStatus.COMPLETED
                        && followUp.getCompletionType()
                                != FollowUpCompletionType.AUTOMATIC) {
                    continue;
                }

                // A later vaccination must exist and be administered
                // on or after the scheduled due date.
                boolean completedByLaterVaccination = vaccinations
                        .subList(i + 1, vaccinations.size())
                        .stream()
                        .anyMatch(laterVaccination ->
                                !laterVaccination.getAdministeredAt()
                                        .toLocalDate()
                                        .isBefore(followUp.getDueDate())
                        );

                if (completedByLaterVaccination) {

                    if (followUp.getStatus() == FollowUpStatus.PENDING) {
                        followUp.markAutomaticallyCompleted();
                    }

                } else {

                    // Reopen only automatically completed records.
                    followUp.reopenAutomaticallyCompleted();
                }
            }
        }
    }

    
    // Create or update a follow-up generated from a visit.
    // Reuse cancelled records instead of creating duplicates.
    // Preserve completed follow-ups.
    @Transactional
    public FollowUp createOrUpdateFromVisit(
            Pet pet,
            String procedureType,
            LocalDate dueDate,
            Long visitId) {

        List<FollowUp> existingFollowUps = followUpRepository
                .findBySourceTypeAndSourceRecordIdOrderByIdAsc(
                        VISIT_SOURCE,
                        visitId
                );

        // Update an existing pending follow-up.
        var pendingFollowUp = existingFollowUps.stream()
                .filter(followUp ->
                        followUp.getStatus() == FollowUpStatus.PENDING)
                .findFirst();

        if (pendingFollowUp.isPresent()) {

            FollowUp followUp = pendingFollowUp.get();

            followUp.updateDetails(
                    pet,
                    procedureType,
                    dueDate
            );

            return followUp;
        }

        // Preserve completed follow-ups.
        var completedFollowUp = existingFollowUps.stream()
                .filter(followUp ->
                        followUp.getStatus() == FollowUpStatus.COMPLETED)
                .findFirst();

        if (completedFollowUp.isPresent()) {
            return completedFollowUp.get();
        }

        // Reopen the latest cancelled follow-up.
        var cancelledFollowUp = existingFollowUps.stream()
                .filter(followUp ->
                        followUp.getStatus() == FollowUpStatus.CANCELLED)
                .max(Comparator.comparing(FollowUp::getId));

        if (cancelledFollowUp.isPresent()) {

            FollowUp followUp = cancelledFollowUp.get();

            followUp.updateDetails(
                    pet,
                    procedureType,
                    dueDate
            );

            followUp.reopenCancelled();

            return followUp;
        }

        // Create a new follow-up only if none exists.
        FollowUp newFollowUp = new FollowUp(
                pet,
                procedureType,
                dueDate,
                VISIT_SOURCE,
                visitId
        );

        return followUpRepository.save(newFollowUp);
    }


    // Cancel only pending follow-ups created from a visit.
    @Transactional
    public void cancelFromVisit(Long visitId) {

        followUpRepository
                .findBySourceTypeAndSourceRecordIdAndStatus(
                        VISIT_SOURCE,
                        visitId,
                        FollowUpStatus.PENDING
                )
                .ifPresent(FollowUp::cancel);
    }

    // Synchronize a visit's follow-up with its calculated due date.
    @Transactional
    public void syncFromVisit(
            Pet pet,
            String procedureType,
            LocalDate dueDate,
            Long visitId) {

        if (dueDate == null) {
            cancelFromVisit(visitId);
            return;
        }

        createOrUpdateFromVisit(
                pet,
                procedureType,
                dueDate,
                visitId
        );
    }

    // List all follow-ups belonging to a pet.
    @Transactional(readOnly = true)
    public List<FollowUp> getFollowUpsByPet(Long petId) {
        return followUpRepository.findByPet_IdOrderByDueDateAsc(petId);
    }

    // List pending follow-ups.
    @Transactional(readOnly = true)
    public List<FollowUp> getPendingFollowUps() {
        return followUpRepository.findByStatusOrderByDueDateAsc(
                FollowUpStatus.PENDING
        );
    }
    
    // Manually reschedule a pending follow-up.
    @Transactional
    public FollowUp rescheduleManually(
            Long followUpId,
            LocalDate newDueDate) {

        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Follow-up not found: " + followUpId
                        )
                );

        if (followUp.getStatus() != FollowUpStatus.PENDING) {
            throw new ConflictException(
                    "Only pending follow-ups can be rescheduled."
            );
        }

        if (newDueDate == null) {
            throw new IllegalArgumentException(
                    "New due date must not be null."
            );
        }

        followUp.rescheduleManually(newDueDate);

        return followUp;
    }


    // Manually complete a pending follow-up.
    @Transactional
    public FollowUp completeManually(Long followUpId) {

        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Follow-up not found: " + followUpId
                        )
                );

        if (followUp.getStatus() != FollowUpStatus.PENDING) {
            throw new ConflictException(
                    "Only pending follow-ups can be completed manually."
            );
        }

        followUp.markManuallyCompleted();

        return followUp;
    }
    // Reconcile follow-ups within the same treatment plan.
// Preserve manually completed and cancelled records.
@Transactional
public void reconcileTreatmentPlanFollowUps(List<Visit> planVisits) {

    for (Visit sourceVisit : planVisits) {

        if (sourceVisit.getStatus() != VisitStatus.COMPLETED
                || sourceVisit.getProcedureType() == null) {
            continue;
        }

        List<FollowUp> followUps = followUpRepository
                .findBySourceTypeAndSourceRecordIdOrderByIdAsc(
                        VISIT_SOURCE,
                        sourceVisit.getId()
                );

        for (FollowUp followUp : followUps) {

            if (followUp.getStatus() == FollowUpStatus.CANCELLED) {
                continue;
            }

            if (followUp.getStatus() == FollowUpStatus.COMPLETED
                    && followUp.getCompletionType()
                    != FollowUpCompletionType.AUTOMATIC) {
                continue;
            }

            boolean completedByLaterVisit = planVisits.stream()
                    .anyMatch(laterVisit ->
                            laterVisit.getStatus() == VisitStatus.COMPLETED
                                    && laterVisit.getId() != null
                                    && !laterVisit.getId().equals(sourceVisit.getId())
                                    && laterVisit.getScheduledAt()
                                            .isAfter(sourceVisit.getScheduledAt())
                                    && !laterVisit.getScheduledAt()
                                            .toLocalDate()
                                            .isBefore(followUp.getDueDate())
                                    && laterVisit.getProcedureType() != null
                                    && laterVisit.getProcedureType()
                                            .equalsIgnoreCase(
                                                    followUp.getProcedureType()
                                            )
                    );

            if (completedByLaterVisit) {

                if (followUp.getStatus() == FollowUpStatus.PENDING) {
                    followUp.markAutomaticallyCompleted();
                }

            } else {
                followUp.reopenAutomaticallyCompleted();
            }
        }
    }
}
}






