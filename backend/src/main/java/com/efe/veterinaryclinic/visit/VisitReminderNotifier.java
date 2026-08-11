package com.efe.veterinaryclinic.visit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Best-effort email notification for an upcoming visit reminder.
 * Never allowed to fail or delay the scheduler run: any error here is caught and logged, not propagated.
 * The JavaMailSender bean itself is optional, since Spring Boot's mail autoconfiguration only
 * registers it when spring.mail.host is set — this component must not stop the whole application
 * from starting just because mail isn't configured.
 */
@Component
public class VisitReminderNotifier {

    private static final Logger log = LoggerFactory.getLogger(VisitReminderNotifier.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;

    public VisitReminderNotifier(ObjectProvider<JavaMailSender> mailSenderProvider,
                                  @Value("${app.reminders.enabled:false}") boolean enabled) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
    }

    public void notifyUpcomingVisit(Visit visit) {
        if (!enabled) {
            return;
        }

        String recipient = visit.getPet().getOwner().getEmail();
        if (recipient == null || recipient.isBlank()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Visit reminders are enabled but no JavaMailSender bean is configured " +
                    "(spring.mail.host missing) — skipping reminder for visit {}", visit.getId());
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(recipient);
            mailMessage.setSubject("[Vet Clinic] Appointment reminder for " + visit.getPet().getName());
            mailMessage.setText(
                    "Dear " + visit.getPet().getOwner().getFirstName() + " " + visit.getPet().getOwner().getLastName() + ",\n\n"
                            + "This is a reminder that " + visit.getPet().getName() + " has an appointment scheduled on "
                            + visit.getScheduledAt().format(DATE_TIME_FORMAT) + " with Dr. " + visit.getVet().getName() + ".\n\n"
                            + "Please contact the clinic if you need to reschedule.\n\n"
                            + "Thank you.");
            mailSender.send(mailMessage);
        } catch (MailException ex) {
            log.warn("Failed to send visit reminder email for visit {}", visit.getId(), ex);
        }
    }
}
