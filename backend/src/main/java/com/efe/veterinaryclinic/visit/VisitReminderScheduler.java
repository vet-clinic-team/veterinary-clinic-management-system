package com.efe.veterinaryclinic.visit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily job that reminds pet owners about tomorrow's scheduled visits.
 * Runs inside a transaction because notifying reads lazily-loaded pet/owner
 * associations, which are otherwise unavailable outside a request/transaction scope.
 */
@Component
public class VisitReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(VisitReminderScheduler.class);

    private final VisitRepository visitRepository;
    private final VisitReminderNotifier visitReminderNotifier;

    public VisitReminderScheduler(VisitRepository visitRepository, VisitReminderNotifier visitReminderNotifier) {
        this.visitRepository = visitRepository;
        this.visitReminderNotifier = visitReminderNotifier;
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendTomorrowReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime windowStart = tomorrow.atStartOfDay();
        LocalDateTime windowEnd = tomorrow.plusDays(1).atStartOfDay();

        List<Visit> visits = visitRepository.findByStatusAndScheduledAtBetweenAndReminderSentAtIsNull(
                VisitStatus.SCHEDULED, windowStart, windowEnd);

        List<Visit> remindedVisits = new ArrayList<>();
        for (Visit visit : visits) {
            if (visitReminderNotifier.notifyUpcomingVisit(visit)) {
                visit.markReminderSent();
                remindedVisits.add(visit);
            }
        }

        if (!remindedVisits.isEmpty()) {
            visitRepository.saveAll(remindedVisits);
            log.info("Sent {} visit reminder(s) for {}", remindedVisits.size(), tomorrow);
        }
    }
}
