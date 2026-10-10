
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.pet.Pet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    @Mock
    private FollowUpRepository followUpRepository;

    @Mock
    private Pet pet;

    @InjectMocks
    private FollowUpService followUpService;

    private final Long vaccinationId = 25L;
    private final String procedureType = "TEST_VACCINE";

    @Test
    void createsFollowUpWhenNoPendingRecordExists() {
        LocalDate dueDate = LocalDate.of(2026, 12, 10);

        when(followUpRepository
                .findBySourceTypeAndSourceRecordIdOrderByIdAsc(
                        "VACCINATION",
                        vaccinationId
                ))
                .thenReturn(List.of());

        when(followUpRepository.save(any(FollowUp.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FollowUp result =
                followUpService.createOrUpdateFromVaccination(
                        pet,
                        procedureType,
                        dueDate,
                        vaccinationId
                );

        assertNotNull(result);
        assertEquals(dueDate, result.getDueDate());
        assertEquals(FollowUpStatus.PENDING, result.getStatus());

        verify(followUpRepository).save(any(FollowUp.class));
    }

    @Test
    void updatesExistingPendingFollowUpInsteadOfCreatingAnother() {
        LocalDate newDate = LocalDate.of(2027, 1, 10);

        FollowUp existingFollowUp = mock(FollowUp.class);

        when(existingFollowUp.getStatus())
                .thenReturn(FollowUpStatus.PENDING);

        when(followUpRepository
                .findBySourceTypeAndSourceRecordIdOrderByIdAsc(
                        "VACCINATION",
                        vaccinationId
                ))
                .thenReturn(List.of(existingFollowUp));

        FollowUp result =
                followUpService.createOrUpdateFromVaccination(
                        pet,
                        procedureType,
                        newDate,
                        vaccinationId
                );

        assertSame(existingFollowUp, result);

        verify(existingFollowUp).updateDetails(
                pet,
                procedureType,
                newDate
        );

        verify(followUpRepository, never())
                .save(any(FollowUp.class));
    }

    @Test
    void cancelsPendingFollowUpWhenVaccinationIsCancelled() {
        FollowUp existingFollowUp = mock(FollowUp.class);

        when(followUpRepository
                .findBySourceTypeAndSourceRecordIdAndStatus(
                        "VACCINATION",
                        vaccinationId,
                        FollowUpStatus.PENDING
                ))
                .thenReturn(Optional.of(existingFollowUp));

        followUpService.cancelFromVaccination(vaccinationId);

        verify(existingFollowUp).cancel();
    }

    @Test
    void syncCancelsFollowUpWhenNextDueDateIsNull() {
        FollowUp existingFollowUp = mock(FollowUp.class);

        when(followUpRepository
                .findBySourceTypeAndSourceRecordIdAndStatus(
                        "VACCINATION",
                        vaccinationId,
                        FollowUpStatus.PENDING
                ))
                .thenReturn(Optional.of(existingFollowUp));

        followUpService.syncFromVaccination(
                pet,
                procedureType,
                null,
                vaccinationId
        );

        verify(existingFollowUp).cancel();

        verify(followUpRepository, never())
                .save(any(FollowUp.class));
    }

    @Test
    void syncCreatesFollowUpWhenNextDueDateExists() {
        LocalDate dueDate = LocalDate.of(2026, 12, 10);

        when(followUpRepository
                .findBySourceTypeAndSourceRecordIdOrderByIdAsc(
                        "VACCINATION",
                        vaccinationId
                ))
                .thenReturn(List.of());

        when(followUpRepository.save(any(FollowUp.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        followUpService.syncFromVaccination(
                pet,
                procedureType,
                dueDate,
                vaccinationId
        );

        verify(followUpRepository).save(any(FollowUp.class));
    }
}


