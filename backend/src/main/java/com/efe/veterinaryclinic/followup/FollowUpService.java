
package com.efe.veterinaryclinic.followup;

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

@Service
public class FollowUpService {

    private static final String VACCINATION_SOURCE = "VACCINATION";

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
}




