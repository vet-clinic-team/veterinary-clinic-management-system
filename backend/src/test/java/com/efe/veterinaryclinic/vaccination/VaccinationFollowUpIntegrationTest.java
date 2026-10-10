
package com.efe.veterinaryclinic.vaccination;

import com.efe.veterinaryclinic.followup.FollowUp;
import com.efe.veterinaryclinic.followup.FollowUpRepository;
import com.efe.veterinaryclinic.followup.FollowUpRule;
import com.efe.veterinaryclinic.followup.FollowUpRuleRepository;
import com.efe.veterinaryclinic.followup.FollowUpStatus;
import com.efe.veterinaryclinic.followup.IntervalUnit;
import com.efe.veterinaryclinic.owner.Owner;
import com.efe.veterinaryclinic.owner.OwnerRepository;
import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.efe.veterinaryclinic.security.Role;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationRequest;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationResponse;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationFollowUpIntegrationTest {

    private static final String TEST_VACCINE =
            "INTEGRATION_TEST_VACCINE";

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private FollowUpRuleRepository ruleRepository;

    @Autowired
    private FollowUpRepository followUpRepository;

    @Autowired
    private VaccinationRepository vaccinationRepository;

    @Autowired
    private VaccinationService vaccinationService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void creatingVaccinationAutomaticallyCreatesFollowUp() {

        Pet pet = createTestPet();
        createTestRule();

        VaccinationResponse response = createVaccination(
                pet,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                "TEST-LOT-001"
        );

        refreshDatabase();

        Vaccination saved = findVaccination(response.id());

        assertEquals(
                LocalDate.of(2026, 12, 10),
                saved.getNextDueDate()
        );

        FollowUp followUp = findPendingFollowUp(response.id());

        assertEquals(
                LocalDate.of(2026, 12, 10),
                followUp.getDueDate()
        );

        assertEquals(
                pet.getId(),
                followUp.getPet().getId()
        );

        assertEquals(
                TEST_VACCINE,
                followUp.getProcedureType()
        );

        assertEquals(
                FollowUpStatus.PENDING,
                followUp.getStatus()
        );
    }

    @Test
    void secondVaccinationUsesSecondRuleStep() {

        Pet pet = createTestPet();
        createTestRule();

        VaccinationResponse first = createVaccination(
                pet,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                "TEST-LOT-001"
        );

        refreshDatabase();

        assertEquals(
                LocalDate.of(2026, 12, 10),
                findVaccination(first.id()).getNextDueDate()
        );

        VaccinationResponse second = createVaccination(
                pet,
                LocalDateTime.of(2026, 12, 10, 10, 0),
                "TEST-LOT-002"
        );

        refreshDatabase();

        assertEquals(
                LocalDate.of(2027, 12, 10),
                findVaccination(second.id()).getNextDueDate()
        );

        FollowUp secondFollowUp =
                findPendingFollowUp(second.id());

        assertEquals(
                LocalDate.of(2027, 12, 10),
                secondFollowUp.getDueDate()
        );

        FollowUp firstFollowUp =
                findFollowUpByVaccinationId(first.id());

        assertEquals(
                FollowUpStatus.COMPLETED,
                firstFollowUp.getStatus()
        );

        assertEquals(
                LocalDate.of(2026, 12, 10),
                firstFollowUp.getDueDate()
        );

        assertNotEquals(
                firstFollowUp.getId(),
                secondFollowUp.getId()
        );
    }

    @Test
    void differentVaccineDoesNotCompleteExistingFollowUp() {

        Pet pet = createTestPet();
        createTestRule();

        VaccinationResponse first = createVaccination(
                pet,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                "TEST-LOT-001"
        );

        // Register a different vaccine type.
        vaccinationService.create(
                new VaccinationRequest(
                        pet.getId(),
                        "DIFFERENT_TEST_VACCINE",
                        LocalDateTime.of(2026, 12, 10, 10, 0),
                        "OTHER-LOT",
                        "Test Veterinarian"
                ),
                Role.ADMIN
        );

        refreshDatabase();

        assertEquals(
                FollowUpStatus.PENDING,
                findPendingFollowUp(first.id()).getStatus()
        );
    }

    @Test
    void differentPetDoesNotCompleteExistingFollowUp() {

        Pet firstPet = createTestPet();
        Pet secondPet = createTestPet();
        createTestRule();

        VaccinationResponse first = createVaccination(
                firstPet,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                "TEST-LOT-001"
        );

        createVaccination(
                secondPet,
                LocalDateTime.of(2026, 12, 10, 10, 0),
                "TEST-LOT-002"
        );

        refreshDatabase();

        assertEquals(
                FollowUpStatus.PENDING,
                findPendingFollowUp(first.id()).getStatus()
        );
    }

    @Test
    void backdatedVaccinationDoesNotCompleteFutureFollowUp() {

        Pet pet = createTestPet();
        createTestRule();

        VaccinationResponse december = createVaccination(
                pet,
                LocalDateTime.of(2026, 12, 10, 10, 0),
                "TEST-LOT-001"
        );

        createVaccination(
                pet,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                "TEST-LOT-OLDER"
        );

        refreshDatabase();

        // A backdated dose must not complete a later follow-up.
        assertEquals(
                FollowUpStatus.PENDING,
                findPendingFollowUp(december.id()).getStatus()
        );
    }

    @Test
    void backdatedVaccinationRecalculatesLaterDose() {

        Pet pet = createTestPet();
        createTestRule();

        // Register December first.
        VaccinationResponse december = createVaccination(
                pet,
                LocalDateTime.of(2026, 12, 10, 10, 0),
                "DECEMBER-LOT"
        );

        assertEquals(
                LocalDate.of(2027, 2, 10),
                findVaccination(december.id()).getNextDueDate()
        );

        // Register an older October dose afterward.
        VaccinationResponse october = createVaccination(
                pet,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                "OCTOBER-LOT"
        );

        refreshDatabase();

        // October is now the first chronological dose.
        assertEquals(
                LocalDate.of(2026, 12, 10),
                findVaccination(october.id()).getNextDueDate()
        );

        // December must be recalculated as the second dose.
        assertEquals(
                LocalDate.of(2027, 12, 10),
                findVaccination(december.id()).getNextDueDate()
        );

        assertEquals(
                LocalDate.of(2027, 12, 10),
                findPendingFollowUp(december.id()).getDueDate()
        );
    }

    @Test
    void deletingEarlierVaccinationRecalculatesRemainingDose() {

        Pet pet = createTestPet();
        createTestRule();

        VaccinationResponse october = createVaccination(
                pet,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                "OCTOBER-LOT"
        );

        VaccinationResponse december = createVaccination(
                pet,
                LocalDateTime.of(2026, 12, 10, 10, 0),
                "DECEMBER-LOT"
        );

        assertEquals(
                LocalDate.of(2027, 12, 10),
                findVaccination(december.id()).getNextDueDate()
        );

        // Delete the earlier vaccination.
        vaccinationService.delete(
                october.id(),
                Role.ADMIN
        );

        refreshDatabase();

        assertTrue(
                vaccinationRepository.findById(october.id()).isEmpty()
        );

        // December becomes the first remaining dose.
        assertEquals(
                LocalDate.of(2027, 2, 10),
                findVaccination(december.id()).getNextDueDate()
        );

        // Its pending follow-up must reflect the new date.
        assertEquals(
                LocalDate.of(2027, 2, 10),
                findPendingFollowUp(december.id()).getDueDate()
        );
    }

    @Test
    void updatingVaccinationDateRecalculatesFollowUp() {

        Pet pet = createTestPet();
        createTestRule();

        VaccinationResponse first = createVaccination(
                pet,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                "TEST-LOT-001"
        );

        // Move the vaccination to November.
        vaccinationService.update(
                first.id(),
                new VaccinationRequest(
                        pet.getId(),
                        TEST_VACCINE,
                        LocalDateTime.of(2026, 11, 10, 10, 0),
                        "TEST-LOT-001",
                        "Test Veterinarian"
                ),
                Role.ADMIN
        );

        refreshDatabase();

        assertEquals(
                LocalDate.of(2027, 1, 10),
                findVaccination(first.id()).getNextDueDate()
        );

        assertEquals(
                LocalDate.of(2027, 1, 10),
                findPendingFollowUp(first.id()).getDueDate()
        );
    }

    @Test
    void recalculationDoesNotReopenCompletedFollowUp() {

        Pet pet = createTestPet();
        createTestRule();

        VaccinationResponse october = createVaccination(
                pet,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                "OCTOBER-LOT"
        );

        VaccinationResponse december = createVaccination(
                pet,
                LocalDateTime.of(2026, 12, 10, 10, 0),
                "DECEMBER-LOT"
        );

        refreshDatabase();

        // The second dose completes the first follow-up.
        assertEquals(
                FollowUpStatus.COMPLETED,
                findFollowUpByVaccinationId(october.id()).getStatus()
        );

        // Updating the later dose triggers recalculation.
        vaccinationService.update(
                december.id(),
                new VaccinationRequest(
                        pet.getId(),
                        TEST_VACCINE,
                        LocalDateTime.of(2026, 12, 15, 10, 0),
                        "DECEMBER-LOT",
                        "Test Veterinarian"
                ),
                Role.ADMIN
        );

        refreshDatabase();

        // The completed follow-up must remain completed.
        FollowUp completed =
                findFollowUpByVaccinationId(october.id());

        assertEquals(
                FollowUpStatus.COMPLETED,
                completed.getStatus()
        );

        // No additional pending follow-up should be created
        // for the already completed October dose.
        long pendingCount = followUpRepository.findAll()
                .stream()
                .filter(followUp ->
                        "VACCINATION".equals(followUp.getSourceType())
                        && october.id().equals(followUp.getSourceRecordId())
                        && followUp.getStatus() == FollowUpStatus.PENDING
                )
                .count();

        assertEquals(0L, pendingCount);
    }
    
    @Test
    void vaccinationsOnSameDayUseCorrectRuleSteps() {

        Pet pet = createTestPet();
        createTestRule();

        VaccinationResponse first = createVaccination(
                pet,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                "MORNING-LOT"
        );

        VaccinationResponse second = createVaccination(
                pet,
                LocalDateTime.of(2026, 10, 10, 14, 0),
                "AFTERNOON-LOT"
        );

        refreshDatabase();

        // The morning dose uses the first rule step.
        assertEquals(
                LocalDate.of(2026, 12, 10),
                findVaccination(first.id()).getNextDueDate()
        );

        // The afternoon dose uses the second rule step.
        assertEquals(
                LocalDate.of(2027, 10, 10),
                findVaccination(second.id()).getNextDueDate()
        );

        // Verify the automatically generated follow-ups.
        assertEquals(
                LocalDate.of(2026, 12, 10),
                findPendingFollowUp(first.id()).getDueDate()
        );

        assertEquals(
                LocalDate.of(2027, 10, 10),
                findPendingFollowUp(second.id()).getDueDate()
        );
    }
    
@Test
void backdatedVaccinationCompletesEarlierFollowUp() {

    Pet pet = createTestPet();
    createTestRule();

    // Register the December vaccination first.
    VaccinationResponse december = createVaccination(
            pet,
            LocalDateTime.of(2026, 12, 10, 10, 0),
            "DECEMBER-LOT"
    );

    // Register the older October vaccination afterward.
    VaccinationResponse october = createVaccination(
            pet,
            LocalDateTime.of(2026, 10, 10, 10, 0),
            "OCTOBER-LOT"
    );

    refreshDatabase();

    // October is the first chronological vaccination.
    assertEquals(
            LocalDate.of(2026, 12, 10),
            findVaccination(october.id()).getNextDueDate()
    );

    // December is the second chronological vaccination.
    assertEquals(
            LocalDate.of(2027, 12, 10),
            findVaccination(december.id()).getNextDueDate()
    );

    // The December vaccination already fulfilled
    // the October follow-up.
    FollowUp octoberFollowUp =
            findFollowUpByVaccinationId(october.id());

    assertEquals(
            FollowUpStatus.COMPLETED,
            octoberFollowUp.getStatus()
    );

    // The December follow-up must remain pending.
    assertEquals(
            FollowUpStatus.PENDING,
            findPendingFollowUp(december.id()).getStatus()
    );
}

@Test
void deletingLaterVaccinationReopensEarlierFollowUp() {

    Pet pet = createTestPet();
    createTestRule();

    // Register the first vaccination.
    VaccinationResponse october = createVaccination(
            pet,
            LocalDateTime.of(2026, 10, 10, 10, 0),
            "OCTOBER-LOT"
    );

    // Register the second vaccination.
    VaccinationResponse december = createVaccination(
            pet,
            LocalDateTime.of(2026, 12, 10, 10, 0),
            "DECEMBER-LOT"
    );

    refreshDatabase();

    // The December vaccination completes the October follow-up.
    assertEquals(
            FollowUpStatus.COMPLETED,
            findFollowUpByVaccinationId(october.id()).getStatus()
    );

    // Delete the December vaccination.
    vaccinationService.delete(
            december.id(),
            Role.ADMIN
    );

    refreshDatabase();

    // The October follow-up should become pending again.
    FollowUp octoberFollowUp =
            findFollowUpByVaccinationId(october.id());

    assertEquals(
            FollowUpStatus.PENDING,
            octoberFollowUp.getStatus()
    );

    assertEquals(
            LocalDate.of(2026, 12, 10),
            octoberFollowUp.getDueDate()
    );

    // The deleted vaccination must no longer exist.
    assertTrue(
            vaccinationRepository.findById(december.id()).isEmpty()
    );
}

@Test
void movingLaterVaccinationBeforeDueDateReopensFollowUp() {

    Pet pet = createTestPet();
    createTestRule();

    // Register the first vaccination.
    VaccinationResponse october = createVaccination(
            pet,
            LocalDateTime.of(2026, 10, 10, 10, 0),
            "OCTOBER-LOT"
    );

    // Register the second vaccination on the due date.
    VaccinationResponse december = createVaccination(
            pet,
            LocalDateTime.of(2026, 12, 10, 10, 0),
            "DECEMBER-LOT"
    );

    refreshDatabase();

    // The December vaccination completes the October follow-up.
    assertEquals(
            FollowUpStatus.COMPLETED,
            findFollowUpByVaccinationId(october.id()).getStatus()
    );

    // Move the second vaccination before the due date.
    vaccinationService.update(
            december.id(),
            new VaccinationRequest(
                    pet.getId(),
                    TEST_VACCINE,
                    LocalDateTime.of(2026, 11, 1, 10, 0),
                    "NOVEMBER-LOT",
                    "Test Veterinarian"
            ),
            Role.ADMIN
    );

    refreshDatabase();

    // The October follow-up must become pending again.
    FollowUp octoberFollowUp =
            findFollowUpByVaccinationId(october.id());

    assertEquals(
            FollowUpStatus.PENDING,
            octoberFollowUp.getStatus()
    );

    assertEquals(
            LocalDate.of(2026, 12, 10),
            octoberFollowUp.getDueDate()
    );

    // The updated second dose must use the second rule step.
    assertEquals(
            LocalDate.of(2027, 11, 1),
            findVaccination(december.id()).getNextDueDate()
    );
}

@Test
void changingLaterVaccinationTypeReopensEarlierFollowUp() {

    Pet pet = createTestPet();
    createTestRule();

    // Create the first vaccination.
    VaccinationResponse october = createVaccination(
            pet,
            LocalDateTime.of(2026, 10, 10, 10, 0),
            "OCTOBER-LOT"
    );

    // Create the second vaccination.
    VaccinationResponse december = createVaccination(
            pet,
            LocalDateTime.of(2026, 12, 10, 10, 0),
            "DECEMBER-LOT"
    );

    refreshDatabase();

    // The second vaccination completes the first follow-up.
    assertEquals(
            FollowUpStatus.COMPLETED,
            findFollowUpByVaccinationId(october.id()).getStatus()
    );

    // Change the second vaccination to a different vaccine type.
    vaccinationService.update(
            december.id(),
            new VaccinationRequest(
                    pet.getId(),
                    "DIFFERENT_TEST_VACCINE",
                    LocalDateTime.of(2026, 12, 10, 10, 0),
                    "DECEMBER-LOT",
                    "Test Veterinarian"
            ),
            Role.ADMIN
    );

    refreshDatabase();

    // The original vaccine follow-up must reopen.
    FollowUp octoberFollowUp =
            findFollowUpByVaccinationId(october.id());

    assertEquals(
            FollowUpStatus.PENDING,
            octoberFollowUp.getStatus()
    );

    assertEquals(
            LocalDate.of(2026, 12, 10),
            octoberFollowUp.getDueDate()
    );

    // The updated vaccination must have the new type.
    assertEquals(
            "DIFFERENT_TEST_VACCINE",
            findVaccination(december.id()).getVaccineType()
    );

    // No active rule exists for the new vaccine type.
    assertNull(
            findVaccination(december.id()).getNextDueDate()
    );
}

@Test
void vaccinationWithoutActiveRuleDoesNotCreateFollowUp() {

    Pet pet = createTestPet();

    // Do not create a follow-up rule for this vaccine type.
    String vaccineWithoutRule = "NO_RULE_TEST_VACCINE";

    VaccinationResponse response = vaccinationService.create(
            new VaccinationRequest(
                    pet.getId(),
                    vaccineWithoutRule,
                    LocalDateTime.of(2026, 10, 10, 10, 0),
                    "NO-RULE-LOT",
                    "Test Veterinarian"
            ),
            Role.ADMIN
    );

    refreshDatabase();

    // The vaccination must still be saved.
    Vaccination saved = findVaccination(response.id());

    assertEquals(
            vaccineWithoutRule,
            saved.getVaccineType()
    );

    // No automatic next due date should be calculated.
    assertNull(saved.getNextDueDate());

    // No follow-up should be generated.
    long followUpCount = followUpRepository.findAll()
            .stream()
            .filter(followUp ->
                    "VACCINATION".equals(followUp.getSourceType())
                    && response.id().equals(followUp.getSourceRecordId())
            )
            .count();

    assertEquals(0L, followUpCount);
}






    

    private Pet createTestPet() {

        Owner owner = ownerRepository.save(
                new Owner(
                        "Integration",
                        "Test",
                        "1234567890",
                        "vaccination-integration@example.com",
                        "Test Address"
                )
        );

        return petRepository.save(
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
    }

    private void createTestRule() {

        // Test-only intervals, not medical recommendations.
        FollowUpRule rule = new FollowUpRule(
                "CAT",
                TEST_VACCINE,
                "Integration Test Protocol"
        );

        rule.addStep(1, 2, IntervalUnit.MONTH);
        rule.addStep(2, 1, IntervalUnit.YEAR);

        ruleRepository.saveAndFlush(rule);
    }

    private VaccinationResponse createVaccination(
            Pet pet,
            LocalDateTime administeredAt,
            String lotNumber) {

        return vaccinationService.create(
                new VaccinationRequest(
                        pet.getId(),
                        TEST_VACCINE,
                        administeredAt,
                        lotNumber,
                        "Test Veterinarian"
                ),
                Role.ADMIN
        );
    }

    private Vaccination findVaccination(Long vaccinationId) {
        return vaccinationRepository.findById(vaccinationId)
                .orElseThrow();
    }

    private FollowUp findPendingFollowUp(Long vaccinationId) {

        return followUpRepository
                .findBySourceTypeAndSourceRecordIdAndStatus(
                        "VACCINATION",
                        vaccinationId,
                        FollowUpStatus.PENDING
                )
                .orElseThrow();
    }

    private FollowUp findFollowUpByVaccinationId(Long vaccinationId) {

        return followUpRepository.findAll()
                .stream()
                .filter(followUp ->
                        "VACCINATION".equals(followUp.getSourceType())
                        && vaccinationId.equals(followUp.getSourceRecordId())
                )
                .findFirst()
                .orElseThrow();
    }

    private void refreshDatabase() {
        entityManager.flush();
        entityManager.clear();
    }
}




