
package com.efe.veterinaryclinic.notification;

import com.efe.veterinaryclinic.followup.FollowUpRepository;
import com.efe.veterinaryclinic.followup.FollowUpStatus;
import com.efe.veterinaryclinic.notification.dto.NewRecordNotification;
import com.efe.veterinaryclinic.notification.dto.NewRecordType;
import com.efe.veterinaryclinic.notification.dto.NotificationResponse;
import com.efe.veterinaryclinic.notification.dto.PendingFollowUpNotification;
import com.efe.veterinaryclinic.notification.dto.UpcomingAppointmentNotification;
import com.efe.veterinaryclinic.notification.dto.VaccinationDueTodayNotification;
import com.efe.veterinaryclinic.owner.OwnerRepository;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.efe.veterinaryclinic.vaccination.VaccinationRepository;
import com.efe.veterinaryclinic.visit.VisitRepository;
import com.efe.veterinaryclinic.visit.VisitStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NotificationService {

    private static final int UPCOMING_APPOINTMENT_WINDOW_HOURS = 2;
    private static final int NEW_RECORD_WINDOW_HOURS = 24;
    private static final int FOLLOW_UP_WINDOW_DAYS = 7;

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final VisitRepository visitRepository;
    private final VaccinationRepository vaccinationRepository;
    private final FollowUpRepository followUpRepository;

    public NotificationService(
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            VisitRepository visitRepository,
            VaccinationRepository vaccinationRepository,
            FollowUpRepository followUpRepository) {

        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.visitRepository = visitRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.followUpRepository = followUpRepository;
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotifications() {

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<UpcomingAppointmentNotification> upcomingAppointments =
                visitRepository
                        .findByStatusAndScheduledAtBetweenOrderByScheduledAtAsc(
                                VisitStatus.SCHEDULED,
                                now,
                                now.plusHours(UPCOMING_APPOINTMENT_WINDOW_HOURS))
                        .stream()
                        .map(visit -> new UpcomingAppointmentNotification(
                                visit.getId(),
                                visit.getPet().getName(),
                                visit.getVet().getName(),
                                visit.getScheduledAt()))
                        .toList();

        List<VaccinationDueTodayNotification> vaccinationsDueToday =
                vaccinationRepository
                        .findByNextDueDate(today)
                        .stream()
                        .map(vaccination -> new VaccinationDueTodayNotification(
                                vaccination.getPet().getId(),
                                vaccination.getPet().getName(),
                                vaccination.getVaccineType()))
                        .toList();

        List<NewRecordNotification> newRecords =
                buildNewRecords(
                        now.minusHours(NEW_RECORD_WINDOW_HOURS));

        List<PendingFollowUpNotification> pendingFollowUps =
                buildPendingFollowUps(today);

        return new NotificationResponse(
                upcomingAppointments,
                vaccinationsDueToday,
                newRecords,
                pendingFollowUps
        );
    }

    private List<PendingFollowUpNotification> buildPendingFollowUps(
            LocalDate today) {

        LocalDate windowEnd = today.plusDays(FOLLOW_UP_WINDOW_DAYS);

        return followUpRepository
                .findByStatusOrderByDueDateAsc(FollowUpStatus.PENDING)
                .stream()
                .filter(followUp ->
                        !followUp.getDueDate().isAfter(windowEnd))
                .map(followUp -> new PendingFollowUpNotification(
                        followUp.getId(),
                        followUp.getPet().getId(),
                        followUp.getPet().getName(),
                        followUp.getProcedureType(),
                        followUp.getDueDate()))
                .toList();
    }

    private List<NewRecordNotification> buildNewRecords(
            LocalDateTime since) {

        List<NewRecordNotification> newRecords = new ArrayList<>();

        ownerRepository
                .findByCreatedAtGreaterThanEqual(since)
                .forEach(owner ->
                        newRecords.add(new NewRecordNotification(
                                NewRecordType.OWNER,
                                owner.getId(),
                                owner.getFirstName() + " " + owner.getLastName(),
                                owner.getCreatedAt())));

        petRepository
                .findByCreatedAtGreaterThanEqual(since)
                .forEach(pet ->
                        newRecords.add(new NewRecordNotification(
                                NewRecordType.PET,
                                pet.getId(),
                                pet.getName(),
                                pet.getCreatedAt())));

        visitRepository
                .findByCreatedAtGreaterThanEqual(since)
                .forEach(visit ->
                        newRecords.add(new NewRecordNotification(
                                NewRecordType.VISIT,
                                visit.getId(),
                                visit.getPet().getName(),
                                visit.getCreatedAt())));

        return newRecords.stream()
                .sorted(
                        Comparator.comparing(
                                NewRecordNotification::createdAt)
                                .reversed())
                .toList();
    }
}

