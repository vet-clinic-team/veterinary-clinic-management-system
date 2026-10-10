
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.owner.Owner;
import com.efe.veterinaryclinic.owner.OwnerRepository;
import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.pet.PetRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FollowUpPersistenceTest {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private FollowUpRepository followUpRepository;

    @Autowired
    private FollowUpService followUpService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createsUpdatesAndCancelsFollowUpInDatabase() {

        // Create a test owner.
        Owner owner = ownerRepository.save(
                new Owner(
                        "Test",
                        "Owner",
                        "1234567890",
                        "followup-test@example.com",
                        "Test Address"
                )
        );

        // Create a test pet.
        Pet pet = petRepository.save(
                new Pet(
                        owner,
                        "Milo",
                        "CAT",
                        "Mixed",
                        null,
                        LocalDate.of(2023, 5, 10),
                        "MALE",
                        4.5,
                        null,
                        null
                )
        );

        Long vaccinationId = 900001L;

        LocalDate firstDueDate = LocalDate.of(2026, 12, 10);
        LocalDate updatedDueDate = LocalDate.of(2027, 1, 10);

        // Create a follow-up record.
        followUpService.syncFromVaccination(
                pet,
                "TEST_VACCINE",
                firstDueDate,
                vaccinationId
        );

        entityManager.flush();
        entityManager.clear();

        FollowUp created = followUpRepository
                .findBySourceTypeAndSourceRecordIdAndStatus(
                        "VACCINATION",
                        vaccinationId,
                        FollowUpStatus.PENDING
                )
                .orElseThrow();

        assertEquals(firstDueDate, created.getDueDate());
        assertEquals("TEST_VACCINE", created.getProcedureType());
        assertEquals(pet.getId(), created.getPet().getId());

        Long followUpId = created.getId();

        // Update the existing follow-up record.
        followUpService.syncFromVaccination(
                pet,
                "TEST_VACCINE",
                updatedDueDate,
                vaccinationId
        );

        entityManager.flush();
        entityManager.clear();

        FollowUp updated = followUpRepository
                .findById(followUpId)
                .orElseThrow();

        assertEquals(updatedDueDate, updated.getDueDate());
        assertEquals(FollowUpStatus.PENDING, updated.getStatus());

        // Cancel the follow-up when no next date exists.
        followUpService.syncFromVaccination(
                pet,
                "TEST_VACCINE",
                null,
                vaccinationId
        );

        entityManager.flush();
        entityManager.clear();

        FollowUp cancelled = followUpRepository
                .findById(followUpId)
                .orElseThrow();

        assertEquals(FollowUpStatus.CANCELLED, cancelled.getStatus());

        // Verify that no pending record remains.
        assertTrue(
                followUpRepository
                        .findBySourceTypeAndSourceRecordIdAndStatus(
                                "VACCINATION",
                                vaccinationId,
                                FollowUpStatus.PENDING
                        )
                        .isEmpty()
        );
    }
}

