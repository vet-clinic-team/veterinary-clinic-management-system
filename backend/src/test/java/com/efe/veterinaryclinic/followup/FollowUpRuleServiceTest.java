
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.vaccination.Vaccination;
import com.efe.veterinaryclinic.vaccination.VaccinationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpRuleServiceTest {

    @Mock
    private FollowUpRuleRepository ruleRepository;

    @Mock
    private FollowUpRuleStepRepository stepRepository;

    @Mock
    private VaccinationRepository vaccinationRepository;

    @InjectMocks
    private FollowUpRuleService followUpRuleService;

    private FollowUpRule rule;

    private final Long petId = 11L;

    private final LocalDate applicationDate =
            LocalDate.of(2026, 10, 10);

    @BeforeEach
    void setUp() {
        rule = new FollowUpRule(
                "CAT",
                "TEST_VACCINE",
                "Test Vaccination Protocol"
        );

        rule.addStep(1, 2, IntervalUnit.MONTH);
        rule.addStep(2, 4, IntervalUnit.MONTH);
    }

    @Test
    void firstApplicationSchedulesSecondDoseAfterTwoMonths() {
        mockRuleAndSteps();

        mockPreviousApplications(applicationDate, 0L);

        Optional<LocalDate> result =
                followUpRuleService.calculateNextDueDate(
                        petId,
                        "CAT",
                        "TEST_VACCINE",
                        applicationDate
                );

        assertEquals(
                LocalDate.of(2026, 12, 10),
                result.orElseThrow()
        );
    }

    @Test
    void secondApplicationSchedulesThirdDoseAfterFourMonths() {
        mockRuleAndSteps();

        LocalDate date = LocalDate.of(2026, 12, 10);

        mockPreviousApplications(date, 1L);

        Optional<LocalDate> result =
                followUpRuleService.calculateNextDueDate(
                        petId,
                        "CAT",
                        "TEST_VACCINE",
                        date
                );

        assertEquals(
                LocalDate.of(2027, 4, 10),
                result.orElseThrow()
        );
    }

    @Test
    void completedProtocolWithoutRepetitionHasNoNextDate() {
        mockRuleAndSteps();

        LocalDate date = LocalDate.of(2027, 4, 10);

        mockPreviousApplications(date, 2L);

        Optional<LocalDate> result =
                followUpRuleService.calculateNextDueDate(
                        petId,
                        "CAT",
                        "TEST_VACCINE",
                        date
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void completedProtocolWithRepetitionSchedulesNextDate() {
        rule.configureRepetition(1, IntervalUnit.YEAR);

        mockRuleAndSteps();

        LocalDate date = LocalDate.of(2027, 4, 10);

        mockPreviousApplications(date, 2L);

        Optional<LocalDate> result =
                followUpRuleService.calculateNextDueDate(
                        petId,
                        "CAT",
                        "TEST_VACCINE",
                        date
                );

        assertEquals(
                LocalDate.of(2028, 4, 10),
                result.orElseThrow()
        );
    }

    @Test
    void repeatedApplicationContinuesUsingRepeatInterval() {
        rule.configureRepetition(1, IntervalUnit.YEAR);

        mockRuleAndSteps();

        LocalDate date = LocalDate.of(2028, 4, 10);

        mockPreviousApplications(date, 3L);

        Optional<LocalDate> result =
                followUpRuleService.calculateNextDueDate(
                        petId,
                        "CAT",
                        "TEST_VACCINE",
                        date
                );

        assertEquals(
                LocalDate.of(2029, 4, 10),
                result.orElseThrow()
        );
    }

    @Test
    void unknownProcedureDoesNotGenerateNextDate() {
        when(ruleRepository
                .findFirstBySpeciesIgnoreCaseAndProcedureTypeIgnoreCaseAndActiveTrue(
                        "CAT",
                        "UNKNOWN_VACCINE"
                ))
                .thenReturn(Optional.empty());

        Optional<LocalDate> result =
                followUpRuleService.calculateNextDueDate(
                        petId,
                        "CAT",
                        "UNKNOWN_VACCINE",
                        applicationDate
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void updatingVaccinationDoesNotCountCurrentRecord() {
        mockRuleAndSteps();

        Long vaccinationId = 25L;

        LocalDateTime administeredAt =
                LocalDateTime.of(2026, 10, 10, 10, 0);

        Vaccination currentVaccination =
                mock(Vaccination.class);
                when(currentVaccination.getId())
        .thenReturn(vaccinationId);

        when(currentVaccination.getAdministeredAt())
                .thenReturn(administeredAt);

        when(vaccinationRepository.findById(vaccinationId))
                .thenReturn(Optional.of(currentVaccination));

        when(vaccinationRepository
                .findByPet_IdAndVaccineTypeIgnoreCaseOrderByAdministeredAtAscIdAsc(
                        petId,
                        "TEST_VACCINE"
                ))
                .thenReturn(List.of(currentVaccination));

        Optional<LocalDate> result =
                followUpRuleService.calculateNextDueDateForUpdate(
                        vaccinationId,
                        petId,
                        "CAT",
                        "TEST_VACCINE",
                        applicationDate
                );

        assertEquals(
                LocalDate.of(2026, 12, 10),
                result.orElseThrow()
        );
    }

    private void mockPreviousApplications(
            LocalDate date,
            long count) {

        when(vaccinationRepository
                .countByPet_IdAndVaccineTypeIgnoreCaseAndAdministeredAtBefore(
                        petId,
                        "TEST_VACCINE",
                        date.atStartOfDay()
                ))
                .thenReturn(count);
    }

    private void mockRuleAndSteps() {
        when(ruleRepository
                .findFirstBySpeciesIgnoreCaseAndProcedureTypeIgnoreCaseAndActiveTrue(
                        eq("CAT"),
                        eq("TEST_VACCINE")
                ))
                .thenReturn(Optional.of(rule));

        when(stepRepository
                .findByRuleIdOrderByStepNumberAsc(isNull()))
                .thenReturn(rule.getSteps());
    }
}


